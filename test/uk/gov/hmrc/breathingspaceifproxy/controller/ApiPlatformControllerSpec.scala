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

package uk.gov.hmrc.breathingspaceifproxy.controller

import scala.concurrent.Future

import controllers.{Assets, Execution}
import org.mockito.scalatest.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.{ActionBuilder, AnyContent, Request}
import play.api.mvc.Results.Status
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.BreathingSpaceConnectorHelper
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class ApiPlatformControllerSpec
    extends BaseSpec
    with BreathingSpaceConnectorHelper
    with MockitoSugar
    with GuiceOneAppPerSuite {

  private val whitelistedAppIdsConfigs = Map(
    "api.access.version-1.0.whitelistedApplicationIds.0" -> "123456789",
    "api.access.version-1.0.whitelistedApplicationIds.1" -> "987654321"
  )

  private val expectedStatus = 200
  private val Action: ActionBuilder[Request, AnyContent] = new ActionBuilder.IgnoringBody()(Execution.trampoline)
  private val fakeRequest = FakeRequest("GET", "/")
  private val mockAssets = mock[Assets]
  private val controller = new ApiPlatformController(
    mockAssets,
    Helpers.stubControllerComponents(),
    play.api.Configuration.from(whitelistedAppIdsConfigs)
  )

  "getDefinition" should {
    "return a definitions.json object with the whitelisted applicationIds included" in {
      Given("a request from the API Platform is received")
      val result = controller.getDefinition()(fakeRequest)

      Then(s"the resulting Response should have as Http Status $expectedStatus")
      status(result) shouldBe expectedStatus

      And(s"a response body with a mime type of ${MimeTypes.JSON}")
      contentType(result) shouldBe Some(MimeTypes.JSON)

      And(s"the response body should contain the correct 'whitelistedApplicationIds' values")
      val versions = (contentAsJson(result) \ "api" \ "versions")
      (versions.head \ "access" \ "whitelistedApplicationIds").as[Seq[String]] shouldBe
        whitelistedAppIdsConfigs.values.toSeq
    }
  }

  "conf" should {
    "return the specified file located in the public resources folder" in {
      val version = "1.0"
      val file = "application.conf"
      when(mockAssets.at(s"/api/conf/$version", file)).thenReturn(Action.async(Future.successful(Status(200))))

      Given("a request from the API Platform is received")
      val result = controller.conf("1.0", "application.conf")(fakeRequest)

      Then(s"the resulting Response should have as Http Status $expectedStatus")
      status(result) shouldBe expectedStatus
    }
  }
}
