/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.ZonedDateTime

import org.scalatest.funsuite.AnyFunSuite
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class TimestampFormatterSpec extends AnyFunSuite with BaseSpec {

  test("timestampFormatter outputs the timestamp according to the expected ISO format") {
    val expectedFormattedDateTime = "2020-12-31T23:59:59.999Z"
    val dateTime                  = ZonedDateTime.parse(expectedFormattedDateTime)
    dateTime.format(timestampFormatter) shouldBe expectedFormattedDateTime
  }
}
