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

package uk.gov.hmrc.breathingspaceifproxy.support

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import cats.syntax.option._
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http.HeaderCarrier

trait BreathingSpaceTestData {

  def appConfig: AppConfig

  val validNinoAsString = "MZ006526C"
  val nino = Nino(validNinoAsString)
  val unknownNino = Nino("MZ005527C")
  val invalidNino = Nino("MG34567")

  val correlationId = UUID.randomUUID
  val correlationIdAsString = correlationId.toString

  val attendedStaffId = "1234567"

  implicit val genericRequestId = RequestId(EndpointId.Breathing_Space_Periods_POST, correlationId)

  lazy val requestHeaders = List(
    CONTENT_TYPE -> MimeTypes.JSON,
    Header.CorrelationId -> correlationIdAsString,
    Header.RequestType -> Attended.DS2_BS_ATTENDED.toString,
    Header.StaffId -> attendedStaffId
  )

  lazy val validDateRangePeriod = RequestPeriod(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(1).some,
    ZonedDateTime.now
  )

  implicit lazy val headerCarrierForIF = HeaderCarrier(
    extraHeaders = List(
      CONTENT_TYPE -> MimeTypes.JSON,
      retrieveHeaderMapping(Header.CorrelationId) -> correlationIdAsString,
      retrieveHeaderMapping(Header.RequestType) -> Attended.DS2_BS_ATTENDED.entryName,
      retrieveHeaderMapping(Header.StaffId) -> attendedStaffId
    )
  )

  lazy val invalidDateRangePeriod = RequestPeriod(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(4).some,
    ZonedDateTime.now
  )

  lazy val validPeriods: List[RequestPeriod] = List(validDateRangePeriod, validDateRangePeriod)

  lazy val validCreatePeriodsRequest = ValidatedCreatePeriodsRequest(nino, validPeriods)

  lazy val invalidCreatePeriodsRequest = ValidatedCreatePeriodsRequest(unknownNino, validPeriods)

  lazy val validPeriodsResponse =
    PeriodsResponse(
      List(
        ResponsePeriod(UUID.randomUUID(), LocalDate.now.minusMonths(5), None),
        ResponsePeriod(UUID.randomUUID(), LocalDate.now.minusMonths(2), LocalDate.now.some)
      )
    )

  def debtorDetails(nino: Nino): String =
    s"""
       |{"nino" : "${nino.value}",
       | "firstName" : "John",
       | "lastName" : "Smith",
       | "dateOfBirth" : "1990-01-01",
       |}
     """.stripMargin

  def retrieveHeaderMapping(header: String): String =
    appConfig.headerMapping.filter(_.nameToMap == header).head.nameMapped
}
