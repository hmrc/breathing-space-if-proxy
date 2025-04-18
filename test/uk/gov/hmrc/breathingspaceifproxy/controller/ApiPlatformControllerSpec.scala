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

package uk.gov.hmrc.breathingspaceifproxy.controller

import controllers.{Assets, Execution}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Status
import play.api.mvc.{ActionBuilder, AnyContent, Request}
import play.api.test.Helpers
import play.api.test.Helpers.*
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

import scala.concurrent.Future

class ApiPlatformControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  def configProperties: Map[String, Any]      = Map(
    "api.access.version-1.0.allowlistedApplicationIds.0" -> "123456789",
    "api.access.version-1.0.allowlistedApplicationIds.1" -> "987654321"
  )
  override def fakeApplication(): Application = GuiceApplicationBuilder().configure(configProperties).build()

  private val expectedStatus                             = 200
  private val Action: ActionBuilder[Request, AnyContent] = new ActionBuilder.IgnoringBody()(Execution.trampoline)
  private val mockAssets                                 = mock[Assets]
  private def controller                                 = new ApiPlatformController(Helpers.stubControllerComponents(), mockAssets)

  "getDefinition" should {
    "return a definitions.json object with the allowlisted applicationIds included" in {

      when(mockAssets.at("/api/definition", "definition.json", false))
        .thenReturn(Action.async(Future.successful(Status(200))))

      Given("a request from the API Platform is received")
      val result = controller.getDefinition()(fakeGetRequest)

      Then(s"the resulting Response should have as Http Status $expectedStatus")
      status(result) shouldBe expectedStatus
    }
  }

  "conf" should {
    "return the specified file located in the public resources folder" in {
      val version = "1.0"
      val file    = "application.conf"
      when(mockAssets.at(s"/api/conf/$version", file)).thenReturn(Action.async(Future.successful(Status(200))))

      Given("a request from the API Platform is received")
      val result = controller.conf("1.0", "application.conf")(fakeGetRequest)

      Then(s"the resulting Response should have as Http Status $expectedStatus")
      status(result) shouldBe expectedStatus
    }
  }
}
