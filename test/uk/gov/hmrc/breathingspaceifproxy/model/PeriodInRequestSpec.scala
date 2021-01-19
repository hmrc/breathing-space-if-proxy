/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.breathingspaceifproxy.model

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import cats.syntax.option._
import org.scalatest.funsuite.AnyFunSuite
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class PeriodInRequestSpec extends AnyFunSuite with BaseSpec {

  val startDate = "2020-01-01"
  val endDate = "2020-03-01"

  val datetime = "2020-11-30T16:43:53"
  val millis = ".000"
  val utc = "Z"

  val expectedPegaRequestTimestamp = s"${datetime}${millis}${utc}"

  val pegaRequestTimestamp1 = s"${datetime}${millis}+01:00"
  val pegaRequestTimestamp2 = s"${datetime}+01:00"

  test("Json.writes for POST BS Periods should format all attributes according to the expected Json") {
    val postPeriod = PostPeriodInRequest(
      LocalDate.parse(startDate),
      LocalDate.parse(endDate).some,
      ZonedDateTime.parse(pegaRequestTimestamp1)
    )
    val res = PostPeriodInRequest.writes.writes(postPeriod)
    (res \ "startDate").as[String] shouldBe startDate
    (res \ "endDate").as[String] shouldBe endDate
    (res \ "pegaRequestTimestamp").as[String] shouldBe expectedPegaRequestTimestamp
  }

  test("Json.writes for POST BS Periods should format all attributes but excluding None values") {
    val postPeriod = PostPeriodInRequest(
      LocalDate.parse(startDate),
      none,
      ZonedDateTime.parse(pegaRequestTimestamp1)
    )
    val res = PostPeriodInRequest.writes.writes(postPeriod)
    (res \ "startDate").as[String] shouldBe startDate
    assert((res \ "endDate").isEmpty)
    (res \ "pegaRequestTimestamp").as[String] shouldBe expectedPegaRequestTimestamp
  }

  test("Json.writes for POST BS Periods should add, for the timestamp, the millisecs when missing") {
    val postPeriod = PostPeriodInRequest(
      LocalDate.parse(startDate),
      none,
      ZonedDateTime.parse(pegaRequestTimestamp2)
    )
    val res = PostPeriodInRequest.writes.writes(postPeriod)
    (res \ "pegaRequestTimestamp").as[String] shouldBe expectedPegaRequestTimestamp
  }

  test("Json.writes for PUT BS Periods should format all attributes according to the expected Json") {
    val putPeriod = PutPeriodInRequest(
      periodId,
      LocalDate.parse(startDate),
      LocalDate.parse(endDate).some,
      ZonedDateTime.parse(pegaRequestTimestamp1)
    )
    val res = PutPeriodInRequest.writes.writes(putPeriod)
    (res \ "periodID").as[UUID] shouldBe periodId
    (res \ "startDate").as[String] shouldBe startDate
    (res \ "endDate").as[String] shouldBe endDate
    (res \ "pegaRequestTimestamp").as[String] shouldBe expectedPegaRequestTimestamp
  }

  test("Json.writes for PUT BS Periods should format all attributes but excluding None values") {
    val putPeriod = PutPeriodInRequest(
      periodId,
      LocalDate.parse(startDate),
      none,
      ZonedDateTime.parse(pegaRequestTimestamp1)
    )
    val res = PutPeriodInRequest.writes.writes(putPeriod)
    (res \ "periodID").as[UUID] shouldBe periodId
    (res \ "startDate").as[String] shouldBe startDate
    assert((res \ "endDate").isEmpty)
    (res \ "pegaRequestTimestamp").as[String] shouldBe expectedPegaRequestTimestamp
  }

  test("Json.writes for PUT BS Periods should add, for the timestamp, the millisecs when missing") {
    val putPeriod = PutPeriodInRequest(
      periodId,
      LocalDate.parse(startDate),
      none,
      ZonedDateTime.parse(pegaRequestTimestamp2)
    )
    val res = PutPeriodInRequest.writes.writes(putPeriod)
    (res \ "pegaRequestTimestamp").as[String] shouldBe expectedPegaRequestTimestamp
  }
}
