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

package uk.gov.hmrc.breathingspaceifproxy.controller.service

import org.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import play.api.mvc.{AnyContent, ControllerComponents, Results}
import play.api.test.Helpers.status
import play.api.test.{Helpers => PlayHelpers}
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

import scala.concurrent.Future

class RequestFeatureFlagSpec extends AnyWordSpec with BaseSpec with RequestFeatureFlag with Results with MockitoSugar {

  val controllerComponents: ControllerComponents = PlayHelpers.stubControllerComponents()
  override implicit val appConfig: AppConfig     = mock[AppConfig]

  "authAction" should {

    "return 200(OK) when feature is enabled" in {

      when(appConfig.memorandumFeatureEnabled).thenReturn(true)

      val result =
        enabled(_.memorandumFeatureEnabled).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe OK
    }

    "return 501(NOT_IMPLEMENTED) when feature is disabled" in {

      when(appConfig.memorandumFeatureEnabled).thenReturn(false)

      val result =
        enabled(_.memorandumFeatureEnabled).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe NOT_IMPLEMENTED
    }
  }
}
