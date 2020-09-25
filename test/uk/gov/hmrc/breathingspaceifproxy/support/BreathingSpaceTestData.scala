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
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http.HeaderCarrier

trait BreathingSpaceTestData {

  val maybeNino = "MZ006526C"
  val nino = Nino(maybeNino)
  val unknownNino = Nino("MZ005527C")
  val invalidNino = Nino("MG34567")

  implicit val requestId = UUID.randomUUID
  val correlationIdAsString = requestId.toString
  val unattendedStaffId = "0000000"

  lazy val requestHeaders = List(
    CONTENT_TYPE -> MimeTypes.JSON,
    Header.CorrelationId -> correlationIdAsString,
    Header.RequestType -> Attended.DS2_BS_ATTENDED.toString,
    Header.StaffId -> "1234567"
  )

  implicit lazy val headerCarrier = HeaderCarrier(
    otherHeaders = List(
      CONTENT_TYPE -> MimeTypes.JSON,
      Header.CorrelationId -> correlationIdAsString,
      Header.RequestType -> Attended.DS2_BS_UNATTENDED.entryName,
      Header.StaffId -> unattendedStaffId
    )
  )

  lazy val validDateRangePeriod = RequestPeriod(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(1).some,
    ZonedDateTime.now
  )

  lazy val invalidDateRangePeriod = RequestPeriod(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(4).some,
    ZonedDateTime.now
  )

  lazy val validPeriods: List[RequestPeriod] = List(validDateRangePeriod, validDateRangePeriod)

  val validCreatePeriodsResponse =
    """
      |{
      | "periods": [
      |   {
      |     "periodId": "76f31303-3336-440c-a2d8-7608be1c32d2",
      |     "startDate": "2020-09-12",
      |     "endDate": "2020-12-13"
      |   },
      |   {
      |     "periodId": "c6743de1-28d4-43ab-9a26-978d2f5157b9",
      |     "startDate": "2020-09-10",
      |     "endDate": "2020-12-11"
      |   }
      | ]
      |}
    """.stripMargin

  def debtorDetails(nino: Nino): String =
    s"""
       |{"nino" : "${nino.value}",
       | "firstName" : "John",
       | "lastName" : "Smith",
       | "dateOfBirth" : "1990-01-01",
       |}
     """.stripMargin
}
