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

package uk.gov.hmrc.breathingspaceifproxy.controller

import play.api.http.Status
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.ApiPlatformController.{conf, getDefinition}
import uk.gov.hmrc.breathingspaceifproxy.support.BaseISpec

class ApiPlatformControllerISpec extends BaseISpec {

  "GET /api/definition" should {
    "return 200(OK) and the definitions.json content" in {
      val url = getDefinition.url
      val response = route(app, fakeAttendedRequest(Helpers.GET, url)).get

      status(response) shouldBe Status.OK

      val appId = appConfig.v1AllowlistedApplicationIds.head
      contentAsString(response) should include(s""""whitelistedApplicationIds": ["${appId}"""")
    }
  }

  "GET /api/conf/1.0/application.raml" should {
    "return 200(OK) and the application.raml content" in {
      val url = conf("1.0", "application.raml").url
      val response = route(app, fakeAttendedRequest(Helpers.GET, url)).get

      status(response) shouldBe Status.OK
      contentAsString(response) should include("title: Breathing Space")
    }
  }
}
