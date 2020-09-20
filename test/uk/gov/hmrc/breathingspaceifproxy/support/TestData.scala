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

import cats.syntax.option._
import play.api.libs.json._
import play.api.mvc.AnyContent
import uk.gov.hmrc.breathingspaceifproxy.RequestedPeriods
import uk.gov.hmrc.breathingspaceifproxy.model.{CreatePeriodsRequest, Nino, RequestedPeriod}

trait TestData {

  val maybeNino = "MZ006526C"
  val nino = Nino(maybeNino)
  val unknownNino = Nino("MZ006526C")

  lazy val period = RequestedPeriod(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(1).some,
    ZonedDateTime.now
  )

  lazy val periods: List[RequestedPeriod] = List(period, period)

  def createPeriodsRequest(nino: String, periods: RequestedPeriods): AnyContent =
    AnyContent(Json.toJson(CreatePeriodsRequest(nino, periods)))

  def debtorDetailsResponse(nino: Nino): String =
    s"""
       |{"nino" : "${nino.value}",
       | "firstName" : "John",
       | "lastName" : "Smith",
       | "dateOfBirth" : "1990-01-01",
       |}
     """.stripMargin

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

  val validCreatePeriodsResponse2 = """
    |{
    | "periods":[
    |   {
    |     "periodId":"12334",
    |     "startDate":"2020-09-19",
    |     "endDate":"2020-09-19"
    |   },
    |   {
    |     "periodId":"98765",
    |     "startDate":"2020-09-19",
    |     "endDate":"2020-09-19"
    |   }
    | ]
    |}""".stripMargin
}
