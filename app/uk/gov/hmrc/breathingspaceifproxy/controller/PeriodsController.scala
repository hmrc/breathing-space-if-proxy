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

import java.time.{LocalDate, LocalTime, ZonedDateTime}

import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._

@Singleton()
class PeriodsController @Inject()(appConfig: AppConfig, cc: ControllerComponents, periodsConnector: PeriodsConnector)
    extends BaseController(appConfig, cc) {

  val post: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    (
      validateHeaders,
      validateBody[CreatePeriodsRequest, ValidatedCreatePeriodsRequest](validateCreatePeriods(_))
    ).mapN((_, vcpr) => vcpr)
      .fold[scala.concurrent.Future[play.api.mvc.Result]](
        ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
        vcpr => {
          logger.debug(s"Creating Periods for $vcpr")
          periodsConnector.post(vcpr)
        }
      )
  }

  private def validateCreatePeriods(cpr: CreatePeriodsRequest): Validation[ValidatedCreatePeriodsRequest] =
    (
      validateNino(cpr.nino),
      validatePeriods(cpr.periods)
    ).mapN((nino, _) => ValidatedCreatePeriodsRequest(nino, cpr.periods))

  private def validatePeriods(periods: Periods): Validation[Unit] =
    periods.map(validatePeriod).combineAll

  private def validatePeriod(period: Period): Validation[Unit] =
    period.endDate.fold {
      (
        validateDate(period.startDate),
        validateDateTime(period.pegaRequestTimestamp)
      ).mapN((_, _) => unit)
    } { endDate =>
      (
        validateDate(period.startDate),
        validateDate(endDate),
        validateDateTime(period.pegaRequestTimestamp)
      ).mapN((startDate, endDate, _) => validateDateRange(startDate, endDate))
    }

  private val BreathingSpaceProgramStartingYear = 2020

  private def validateDate(date: LocalDate): Validation[LocalDate] =
    if (date.getYear >= BreathingSpaceProgramStartingYear) date.validNec
    else Error(INVALID_DATE, s". Year(${date.getYear}) is before $BreathingSpaceProgramStartingYear".some).invalidNec

  private val seconds = 60

  private def validateDateTime(requestDateTime: ZonedDateTime): Validation[Unit] =
    if (LocalTime.now.minusSeconds(seconds).isAfter(requestDateTime.toLocalTime)) unit.validNec
    else Error(INVALID_DATE, s". Request's date-time is too old (more than $seconds seconds)".some).invalidNec

  private def validateDateRange(startDate: LocalDate, endDate: LocalDate): Validation[Unit] =
    if (startDate.isBefore(endDate)) unit.validNec
    else Error(INVALID_DATE_RANGE, s". startDate($startDate) is after endDate($endDate)".some).invalidNec
}
