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

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import scala.concurrent.ExecutionContext.Implicits.global

import cats.syntax.apply._
import cats.syntax.option._
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
    ).mapN((correlationId, nino, _) => (RequestId(Breathing_Space_Periods_GET, correlationId), nino))
      .fold(
        ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
        validationTuple => {
          implicit val (requestId, nino) = validationTuple
          logger.debug(s"$requestId for Nino(${nino.value})")
          periodsConnector.get(nino).flatMap {
            _.fold(ErrorResponse(requestId.correlationId, _).value, composeResponse(OK, _))
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
          (RequestId(Breathing_Space_Periods_POST, correlationId), ninoAndPostPeriods._1, ninoAndPostPeriods._2)
      )
      .fold(
        ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
        validationTuple => {
          implicit val (requestId, nino, postPeriodsInRequest) = validationTuple
          logger.debug(s"$requestId with $postPeriodsInRequest for Nino(${nino.value})")
          periodsConnector.post(nino, postPeriodsInRequest).flatMap {
            _.fold(ErrorResponse(requestId.correlationId, _).value, composeResponse(CREATED, _))
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
          (RequestId(Breathing_Space_Periods_POST, correlationId), nino, putPeriodsInRequest)
      )
      .fold(
        ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
        validationTuple => {
          implicit val (requestId, nino, putPeriodsInRequest) = validationTuple
          logger.debug(s"$requestId with $putPeriodsInRequest for Nino(${nino.value})")
          periodsConnector.put(nino, putPeriodsInRequest).flatMap {
            _.fold(ErrorResponse(requestId.correlationId, _).value, composeResponse(OK, _))
          }
        }
      )
  }

  private def validateBodyOfPost(json: JsValue): Validation[(Nino, PostPeriodsInRequest)] =
    (
      validateNino(parseJsValue[String](json, "nino")),
      parseJsValue[JsArray](json, "periods")
        .fold(ErrorItem(MISSING_PERIODS).invalidNec[PostPeriodsInRequest]) {
          validateJsArray[PostPeriodInRequest](_, "period", validatePeriod)
        }
    ).mapN((nino, periods) => (nino, periods))

  private def validateBodyOfPut(json: JsValue): Validation[PutPeriodsInRequest] =
    parseJsValue[JsArray](json, "periods")
      .fold(ErrorItem(MISSING_PERIODS).invalidNec[PutPeriodsInRequest]) {
        validateJsArray[PutPeriodInRequest](_, "period", validatePeriod)
      }

  private def validatePeriod[T <: PeriodInRequest](period: T, itemIndex: Int): Validation[T] =
    period.endDate.fold {
      (
        validateDate(period.startDate, itemIndex),
        validateDateTime(period.pegaRequestTimestamp, itemIndex)
      ).mapN((_, _) => period)
    } { endDate =>
      (
        validateDate(period.startDate, itemIndex),
        validateDate(endDate, itemIndex),
        validateDateRange(period.startDate, endDate, itemIndex),
        validateDateTime(period.pegaRequestTimestamp, itemIndex)
      ).mapN((_, _, _, _) => period)
    }

  private val BreathingSpaceProgramStartingYear = 2020

  private def validateDate(date: LocalDate, itemIndex: Int): Validation[LocalDate] =
    if (date.getYear >= BreathingSpaceProgramStartingYear) date.validNec
    else {
      val details = s"$itemIndex. Year(${date.getYear}) is before $BreathingSpaceProgramStartingYear"
      ErrorItem(INVALID_DATE, details.some).invalidNec
    }

  val timestampLimit = 60

  private def validateDateTime(requestDateTime: ZonedDateTime, itemIndex: Int): Validation[Unit] =
    if (requestDateTime.toLocalDateTime.isBefore(LocalDateTime.now.minusSeconds(timestampLimit))) {
      val details = s"$itemIndex. Request timestamp is too old (more than $timestampLimit seconds)"
      ErrorItem(INVALID_TIMESTAMP, details.some).invalidNec
    } else unit.validNec

  private def validateDateRange(startDate: LocalDate, endDate: LocalDate, itemIndex: Int): Validation[Unit] =
    if (endDate.isBefore(startDate)) {
      val details = s"$itemIndex. startDate($startDate) is after endDate($endDate)"
      ErrorItem(INVALID_DATE_RANGE, details.some).invalidNec
    } else unit.validNec
}
