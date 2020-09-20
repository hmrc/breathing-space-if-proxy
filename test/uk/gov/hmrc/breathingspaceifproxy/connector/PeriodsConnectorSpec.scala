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

package uk.gov.hmrc.breathingspaceifproxy.connector

import java.time.LocalDate

import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector.validateResponseBody
import uk.gov.hmrc.breathingspaceifproxy.model.{IFCreatePeriodsResponse, Period, PeriodID}
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.HttpResponse

class PeriodsConnectorSpec extends AnyWordSpec with BaseSpec {

  "PeriodsConnector.validateResponseBody" should {
    "correctly parse a valid create" in {

      val sample = IFCreatePeriodsResponse(
        List(
          Period(PeriodID("12334"), LocalDate.now(), Some(LocalDate.now())),
          Period(PeriodID("98765"), LocalDate.now(), Some(LocalDate.now()))
        )
      )
      val sampleJson = Json.toJson(sample)
      println(sampleJson)

      val fakeResponse = HttpResponse(Status.CREATED, validCreatePeriodsResponse)

      val eitherOutcome = validateResponseBody[IFCreatePeriodsResponse](fakeResponse)

      eitherOutcome should be('right)
    }
  }
}
