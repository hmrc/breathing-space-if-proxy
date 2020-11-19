/*
 * Copyright 2020 HM Revenue & Customs
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

import scala.concurrent.ExecutionContext.Implicits.global

import cats.syntax.apply._
import cats.syntax.validated._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsArray, JsValue}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.EndpointId._

@Singleton()
class PeriodsController @Inject()(appConfig: AppConfig, cc: ControllerComponents, periodsConnector: PeriodsConnector)
    extends AbstractBaseController(appConfig, cc) {

  def get(maybeNino: String): Action[Validation[AnyContent]] = Action.async(withoutBody) { implicit request =>
    (
      validateHeadersForNPS,
      validateNino(maybeNino),
      request.body
    ).mapN((correlationId, nino, _) => (RequestId(BS_Periods_GET, correlationId), nino))
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino) = validationTuple
          logger.debug(s"$requestId for Nino(${nino.value})")
          if (appConfig.onDevEnvironment) logHeaders
          periodsConnector.get(nino).flatMap {
            _.fold(HttpError(requestId.correlationId, _).send, composeResponse(OK, _))
          }
        }
      )
  }

  val post: Action[Validation[JsValue]] = Action.async(withJsonBody) { implicit request =>
    (
      validateHeadersForNPS,
      request.body.andThen(validateBodyOfPost)
    ).mapN(
        (correlationId, ninoAndPostPeriods) =>
          (RequestId(BS_Periods_POST, correlationId), ninoAndPostPeriods._1, ninoAndPostPeriods._2)
      )
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino, postPeriodsInRequest) = validationTuple
          logger.debug(s"$requestId with $postPeriodsInRequest for Nino(${nino.value})")
          if (appConfig.onDevEnvironment) logHeaders
          periodsConnector.post(nino, postPeriodsInRequest).flatMap {
            _.fold(HttpError(requestId.correlationId, _).send, composeResponse(CREATED, _))
          }
        }
      )
  }

  def put(maybeNino: String): Action[Validation[JsValue]] = Action.async(withJsonBody) { implicit request =>
    (
      validateHeadersForNPS,
      validateNino(maybeNino),
      request.body.andThen(validateBodyOfPut)
    ).mapN(
        (correlationId, nino, putPeriodsInRequest) =>
          (RequestId(BS_Periods_POST, correlationId), nino, putPeriodsInRequest)
      )
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino, putPeriodsInRequest) = validationTuple
          logger.debug(s"$requestId with $putPeriodsInRequest for Nino(${nino.value})")
          if (appConfig.onDevEnvironment) logHeaders
          periodsConnector.put(nino, putPeriodsInRequest).flatMap {
            _.fold(HttpError(requestId.correlationId, _).send, composeResponse(OK, _))
          }
        }
      )
  }

  private def validateBodyOfPost(json: JsValue): Validation[(Nino, PostPeriodsInRequest)] =
    (
      validateNino(parseJsValue[String](json, "nino")),
      parseJsValue[JsArray](json, "periods")
        .fold(ErrorItem(MISSING_PERIODS).invalidNec[PostPeriodsInRequest]) {
          validateJsArray[PostPeriodInRequest](_, "period")
        }
    ).mapN((nino, periods) => (nino, periods))

  private def validateBodyOfPut(json: JsValue): Validation[PutPeriodsInRequest] =
    parseJsValue[JsArray](json, "periods")
      .fold(ErrorItem(MISSING_PERIODS).invalidNec[PutPeriodsInRequest]) {
        validateJsArray[PutPeriodInRequest](_, "period")
      }
}
