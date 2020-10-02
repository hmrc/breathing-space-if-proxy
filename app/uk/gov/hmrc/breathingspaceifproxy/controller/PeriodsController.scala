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
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global

import cats.syntax.apply._
import cats.syntax.foldable._
import cats.syntax.option._
import cats.syntax.validated._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.EndpointId._

@Singleton()
class PeriodsController @Inject()(appConfig: AppConfig, cc: ControllerComponents, periodsConnector: PeriodsConnector)
    extends BaseController(appConfig, cc) {

  def get(maybeNino: String): Action[AnyContent] = Action.async { implicit request =>
    (
      validateHeadersForNPS,
      validateNino(maybeNino)
    ).mapN((correlationId, nino) => (RequestId(Breathing_Space_Periods_GET, correlationId), nino))
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

  val post: Action[AnyContent] = Action.async { implicit request =>
    (
      validateHeadersForNPS,
      validateBody[CreatePeriodsRequest, ValidatedCreatePeriodsRequest](validateCreatePeriods(_))
    ).mapN((correlationId, vcpr) => (RequestId(Breathing_Space_Periods_POST, correlationId), vcpr))
      .fold(
        ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
        validationTuple => {
          implicit val (requestId, vcpr) = validationTuple
          logger.debug(s"$requestId with $vcpr")
          periodsConnector.post(vcpr).flatMap {
            _.fold(ErrorResponse(requestId.correlationId, _).value, composeResponse(CREATED, _))
          }
        }
      )
  }

  private def validateCreatePeriods(cpr: CreatePeriodsRequest): Validation[ValidatedCreatePeriodsRequest] =
    (
      validateNino(cpr.nino),
      validatePeriods(cpr.periods)
    ).mapN((nino, _) => ValidatedCreatePeriodsRequest(nino, cpr.periods))

  private def validatePeriods(periods: RequestPeriods): Validation[Unit] =
    periods.map(validatePeriod).combineAll

  private def validatePeriod(period: RequestPeriod): Validation[Unit] =
    period.endDate.fold {
      (
        validateDate(period.startDate),
        validateDateTime(period.pegaRequestTimestamp)
      ).mapN((_, _) => unit)
    } { endDate =>
      (
        validateDate(period.startDate),
        validateDate(endDate),
        validateDateRange(period.startDate, endDate),
        validateDateTime(period.pegaRequestTimestamp)
      ).mapN((_, _, _, _) => unit)
    }

  private val BreathingSpaceProgramStartingYear = 2020

  private def validateDate(date: LocalDate): Validation[LocalDate] =
    if (date.getYear >= BreathingSpaceProgramStartingYear) date.validNec
    else Error(INVALID_DATE, s". Year(${date.getYear}) is before $BreathingSpaceProgramStartingYear".some).invalidNec

  val timestampLimit = 60

  private def validateDateTime(requestDateTime: ZonedDateTime): Validation[Unit] =
    if (requestDateTime.toLocalDateTime.isBefore(LocalDateTime.now.minusSeconds(timestampLimit))) {
      Error(INVALID_TIMESTAMP, s". Request timestamp is too old (more than $timestampLimit seconds)".some).invalidNec
    } else unit.validNec

  private def validateDateRange(startDate: LocalDate, endDate: LocalDate): Validation[Unit] =
    if (endDate.isBefore(startDate)) {
      Error(INVALID_DATE_RANGE, s". startDate($startDate) is after endDate($endDate)".some).invalidNec
    } else unit.validNec
}
