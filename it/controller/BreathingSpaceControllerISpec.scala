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

package controller

import play.api.http.Status
import play.api.test.Helpers._
import play.api.test.WsTestClient

class BreathingSpaceControllerISpec extends BaseControllerISpec {

  private val anino = "ABCDE"

  WsTestClient.withClient { client =>
    "GET /debtor/:nino/debt-details" in {
      val result = await(
        client.url(s"http://localhost:$port/breathing-space/debtor/${anino}/identity-details")
          .addHttpHeaders("Content-Type" -> "application/json")
          .get()
      )

      result.status shouldBe Status.OK

    }
  }

  /*
  "POST /breathing-space-period" should {
    "return 200" in {
      val result = controller.createBreathingSpacePeriod()(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }*/
}
