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

package uk.gov.hmrc.breathingspaceifproxy.connector

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class UnderpaymentsConnectorSpec extends AnyWordSpec with BaseSpec {

  "UnderpaymentsConnector.url" should {
    "correctly compose a url to the IF" in {
      Given("a valid Nino")
      val nino        = genNino
      val expectedUrl =
        s"http://localhost:9503/${appConfig.integrationFrameworkContext}" +
          s"/breathing-space/${nino.value}/$periodIdAsString/coding-out-debts"

      Then(s"then the composed url should be equal to $expectedUrl")
      UnderpaymentsConnector.url(nino, periodId) shouldBe expectedUrl
    }
  }

}
