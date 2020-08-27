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

package unit.controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.http.Status
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.controller.BreathingSpaceController
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class BreathingSpaceControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  private val fakeRequest = FakeRequest("GET", "/")

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig = new AppConfig(configuration, serviceConfig)

  private val controller = new BreathingSpaceController(appConfig, Helpers.stubControllerComponents())

  "GET /debtor/:nino/debt-details" should {
    "return 200" in {
      val result = controller.retrieveIdentityDetails("fakeNino")(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "POST /breathing-space-period" should {
    "return 200" in {
      val result = controller.createBreathingSpacePeriod()(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}