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
import play.api.libs.json._
import play.api.mvc.AnyContent
import uk.gov.hmrc.breathingspaceifproxy.Periods
import uk.gov.hmrc.breathingspaceifproxy.model._

trait TestData {

  val maybeNino = "MZ006526C"
  val nino = Nino(maybeNino)
  val unknownNino = Nino("MZ006526C")
  val correlationId = CorrelationId(UUID.randomUUID().toString)
  val unattendedStaffId = StaffId("0000000")

  lazy val period = Period(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(1).some,
    ZonedDateTime.now
  )

  lazy val periods: List[Period] = List(period, period)

  def createPeriodsRequest(nino: String, periods: Periods): AnyContent =
    AnyContent(Json.toJson(CreatePeriodsRequest(nino, periods)))

  def debtorDetailsResponse(nino: Nino): String =
    s"""
       |{"nino" : "${nino.value}",
       | "firstName" : "John",
       | "lastName" : "Smith",
       | "dateOfBirth" : "1990-01-01",
       |}
     """.stripMargin
}
