/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.breathingspaceifproxy.controller

import java.util.UUID
import javax.inject.{Inject, Singleton}
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.validated._
import play.api.libs.json.{JsArray, JsValue}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.service.AbstractBaseController
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

@Singleton()
class PeriodsController @Inject()(
  override val appConfig: AppConfig,
  override val auditConnector: AuditConnector,
  override val authConnector: AuthConnector,
  cc: ControllerComponents,
  periodsConnector: PeriodsConnector
) extends AbstractBaseController(cc) {

  val readAction = authAction("read:breathing-space-periods")

  def get(maybeNino: String): Action[Validation[AnyContent]] = readAction.async(withoutBody) { implicit request =>
    (
      validateHeadersForNPS(BS_Periods_GET, periodsConnector.eisConnector),
      validateNino(maybeNino),
      request.body
    ).mapN((requestId, nino, _) => (requestId, nino))
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino) = validationTuple
          logHeadersAndRequestId(nino, requestId)
          periodsConnector.get(nino).flatMap {
            _.fold(auditEventAndSendErrorResponse[AnyContent], auditEventAndSendResponse(OK, _))
          }
        }
      )
  }

  val writeAction = authAction("write:breathing-space-periods")

  val post: Action[Validation[JsValue]] = writeAction.async(withJsonBody) { implicit request =>
    (
      validateHeadersForNPS(BS_Periods_POST, periodsConnector.eisConnector),
      request.body.andThen(validateBodyOfPost)
    ).mapN((requestId, ninoAndPostPeriods) => (requestId, ninoAndPostPeriods._1, ninoAndPostPeriods._2))
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino, postPeriodsInRequest) = validationTuple
          logger.debug(s"$requestId with $postPeriodsInRequest for Nino(${nino.value})")
          logHeaders
          periodsConnector.post(nino, postPeriodsInRequest).flatMap {
            _.fold(auditEventAndSendErrorResponse[JsValue], auditEventAndSendResponse(CREATED, _))
          }
        }
      )
  }

  def put(maybeNino: String): Action[Validation[JsValue]] = writeAction.async(withJsonBody) { implicit request =>
    (
      validateHeadersForNPS(BS_Periods_PUT, periodsConnector.eisConnector),
      validateNino(maybeNino),
      request.body.andThen(validateBodyOfPut)
    ).mapN((requestId, nino, putPeriodsInRequest) => (requestId, nino, putPeriodsInRequest))
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino, putPeriodsInRequest) = validationTuple
          logger.debug(s"$requestId with $putPeriodsInRequest for Nino(${nino.value})")
          logHeaders
          periodsConnector.put(nino, putPeriodsInRequest).flatMap {
            _.fold(auditEventAndSendErrorResponse[JsValue], auditEventAndSendResponse(OK, _))
          }
        }
      )
  }

  private def validateBodyOfPost(json: JsValue): Validation[(Nino, PostPeriodsInRequest)] =
    (
      validateNino(parseJsValue[String](json, "nino")),
      validateConsumerRequestId(parseJsValue[String](json, "consumerRequestId")),
      parseJsValueOpt[String](json, "utr").andThen(validateUtr),
      parseJsValue[JsArray](json, "periods")
        .fold(ErrorItem(MISSING_PERIODS).invalidNec[List[PostPeriodInRequest]]) {
          validateJsArray[PostPeriodInRequest](_, "period")
        }
    ).mapN((nino, consumerRequestId, utr, periods) => (nino, PostPeriodsInRequest(consumerRequestId, utr, periods)))

  private def validateBodyOfPut(json: JsValue): Validation[List[PutPeriodInRequest]] =
    parseJsValue[JsArray](json, "periods")
      .fold(ErrorItem(MISSING_PERIODS).invalidNec[List[PutPeriodInRequest]]) {
        validateJsArray[PutPeriodInRequest](_, "period")
      }

  def validateConsumerRequestId(maybeConsumerRequestId: Option[String]): Validation[UUID] =
    maybeConsumerRequestId.fold(ErrorItem(MISSING_CONSUMER_REQUEST_ID).invalidNec[UUID]) { crId =>
      Either
        .catchNonFatal(UUID.fromString(crId))
        .fold(
          _ => ErrorItem(INVALID_CONSUMER_REQUEST_ID, s"(${DownstreamHeader.CorrelationId})".some).invalidNec[UUID],
          _.validNec[ErrorItem]
        )
    }

  private def validateUtr(maybeUtr: Option[String]): Validation[Option[String]] =
    maybeUtr.fold(none[String].validNec[ErrorItem])(validateUtr)

  private val utrRegex = """^[0-9]{10}$""".r

  private def validateUtr(utr: String): Validation[Option[String]] =
    utr match {
      case utrRegex() => utr.some.validNec[ErrorItem]
      case _ => ErrorItem(INVALID_UTR).invalidNec[Option[String]]
    }
}
