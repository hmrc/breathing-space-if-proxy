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
    ).mapN((correlationId, ninoAndBody) => (RequestId(Breathing_Space_Periods_POST, correlationId), ninoAndBody))
      .fold(
        ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
        validationTuple => {
          implicit val requestId = validationTuple._1
          val (nino, body) = validationTuple._2
          logger.debug(s"$requestId with $body for Nino(${nino.value})")
          periodsConnector.post(nino, body).flatMap {
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
    ).mapN((correlationId, nino, body) => (RequestId(Breathing_Space_Periods_POST, correlationId), nino, body))
      .fold(
        ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
        validationTuple => {
          implicit val (requestId, nino, body) = validationTuple
          logger.debug(s"$requestId with $body for Nino(${nino.value})")
          periodsConnector.put(nino, body).flatMap {
            _.fold(ErrorResponse(requestId.correlationId, _).value, composeResponse(OK, _))
          }
        }
      )
  }

  private def validateBodyOfPost(json: JsValue): Validation[(Nino, PostPeriods)] =
    (
      validateNino(validateJsValue[String](json, "nino")),
      validateJsValue[JsArray](json, "periods")
        .fold(ErrorItem(MISSING_PERIODS).invalidNec[PostPeriods]) {
          validateJsArray[PostPeriod](_, "period", validatePeriod)
        }
    ).mapN((nino, periods) => (nino, periods))

  private def validateBodyOfPut(json: JsValue): Validation[PutPeriods] =
    validateJsValue[JsArray](json, "periods")
      .fold(ErrorItem(MISSING_PERIODS).invalidNec[PutPeriods]) {
        validateJsArray[PutPeriod](_, "period", validatePeriod)
      }

  private def validatePeriod[T <: PeriodInRequest](period: T, ix: Int): Validation[T] =
    period.endDate.fold {
      (
        validateDate(period.startDate, ix),
        validateDateTime(period.pegaRequestTimestamp, ix)
      ).mapN((_, _) => period)
    } { endDate =>
      (
        validateDate(period.startDate, ix),
        validateDate(endDate, ix),
        validateDateRange(period.startDate, endDate, ix),
        validateDateTime(period.pegaRequestTimestamp, ix)
      ).mapN((_, _, _, _) => period)
    }

  private val BreathingSpaceProgramStartingYear = 2020

  private def validateDate(date: LocalDate, ix: Int): Validation[LocalDate] =
    if (date.getYear >= BreathingSpaceProgramStartingYear) date.validNec
    else {
      ErrorItem(INVALID_DATE, s"$ix. Year(${date.getYear}) is before $BreathingSpaceProgramStartingYear".some).invalidNec
    }

  val timestampLimit = 60

  private def validateDateTime(requestDateTime: ZonedDateTime, ix: Int): Validation[Unit] =
    if (requestDateTime.toLocalDateTime.isBefore(LocalDateTime.now.minusSeconds(timestampLimit))) {
      ErrorItem(INVALID_TIMESTAMP, s"$ix. Request timestamp is too old (more than $timestampLimit seconds)".some).invalidNec
    } else unit.validNec

  private def validateDateRange(startDate: LocalDate, endDate: LocalDate, ix: Int): Validation[Unit] =
    if (endDate.isBefore(startDate)) {
      ErrorItem(INVALID_DATE_RANGE, s"$ix. startDate($startDate) is after endDate($endDate)".some).invalidNec
    } else unit.validNec
}
