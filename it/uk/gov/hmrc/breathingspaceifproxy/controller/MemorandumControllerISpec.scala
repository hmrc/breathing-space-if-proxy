/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.breathingspaceifproxy.connector.MemorandumConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.MemorandumController.get
import uk.gov.hmrc.breathingspaceifproxy.model.MemorandumInResponse
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Memorandum_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class MemorandumControllerISpec extends BaseISpec {

  val nino = genNino
  val getPathWithValidNino = get(nino.value).url
  val memorandumConnectorUrl = MemorandumConnector.path(nino)


  "GET BS Memorandum for Nino" should {

    "return 200(OK) for the valid Nino provided" in {
      val expectedResponseBody = Json.toJson(MemorandumInResponse(true)).toString
      stubCall(HttpMethod.Get, memorandumConnectorUrl, Status.OK, expectedResponseBody)

     val response = route(app, fakeUnattendedRequest(Helpers.GET, getPathWithValidNino)).get

      status(response) shouldBe Status.OK
      contentAsString(response) shouldBe expectedResponseBody
      verifyHeadersForUnattended(HttpMethod.Get, memorandumConnectorUrl)
      verifyAuditEventCall(BS_Memorandum_GET)
    }

  }
}
