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

import java.util.UUID

import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.connector.BreathingSpaceConnectorHelper
import uk.gov.hmrc.breathingspaceifproxy.model.Attended
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class BreathingSpaceControllerISpec extends BaseISpec with BreathingSpaceConnectorHelper {

  val identityDetailsPath = s"/$localContext/debtor/${nino.value}/identity-details"

  s"GET /$localContext/debtor/:nino/identity-details" should {

    "return 200(OK) and debtor details when the Nino is valid" in {
      val expectedBody = debtorDetails(nino)

      val connectorUrl = retrieveIdentityDetailsPath(appConfig, nino)
      stubCall(HttpMethod.Get, connectorUrl, Status.OK, expectedBody)

      val result = route(fakeApplication, fakeRequest(Helpers.GET, identityDetailsPath)).get
      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe expectedBody
    }

    "return 400(BadRequest) when the required headers are missing" in {
      val result = route(fakeApplication, FakeRequest(Helpers.GET, identityDetailsPath)).get
      status(result) shouldBe Status.BAD_REQUEST
      reason(result) shouldBe MissingRequiredHeaders
    }

    s"return 400(BadRequest when the $HeaderCorrelationId header is missing" in {
      val request = FakeRequest(Helpers.GET, identityDetailsPath).withHeaders(
        HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
        HeaderContext -> Attended.PEGA_UNATTENDED.toString
      )

      val result = route(fakeApplication, request).get
      status(result) shouldBe Status.BAD_REQUEST
      reason(result) shouldBe MissingRequiredHeaders
    }

    s"return 400(BadRequest when the $HeaderContext header is missing" in {
      val request = FakeRequest(Helpers.GET, identityDetailsPath).withHeaders(
        HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
        HeaderCorrelationId -> UUID.randomUUID.toString
      )

      val result = route(fakeApplication, request).get
      status(result) shouldBe Status.BAD_REQUEST
      reason(result) shouldBe MissingRequiredHeaders
    }

    s"return 400(BadRequest when the $HeaderContext header's value is unknown" in {
      val invalidContext = "INVALID_CONTEXT"
      val request = FakeRequest(Helpers.GET, identityDetailsPath).withHeaders(
        HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
        HeaderCorrelationId -> UUID.randomUUID.toString,
        HeaderContext -> invalidContext
      )

      val result = route(fakeApplication, request).get
      status(result) shouldBe Status.BAD_REQUEST
      reason(result) shouldBe invalidContextHeader(invalidContext)
    }

    "return 404(NotFound) when the Nino is unknown" in {
      val connectorUrl = retrieveIdentityDetailsPath(appConfig, unknownNino)
      stubCall(HttpMethod.Get, connectorUrl, Status.NOT_FOUND, s"Nino(${unknownNino.value}) is unknown")

      val path = s"/$localContext/debtor/${unknownNino.value}/identity-details"
      val result = route(fakeApplication, fakeRequest(Helpers.GET, path)).get
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 422(UnprocessableEntity) when the Nino is invalid" in {
      val request = fakeRequest(Helpers.GET, s"/$localContext/debtor/12345/identity-details")
      val result = route(fakeApplication, request).get
      status(result) shouldBe Status.UNPROCESSABLE_ENTITY
    }

    "return 502(BadGateway) when the connection with I-F fails" in {
      wireMockServer.stop()
      val result = route(fakeApplication, fakeRequest(Helpers.GET, identityDetailsPath)).get
      val httpCode = status(result)
      wireMockServer.start()
      httpCode shouldBe Status.BAD_GATEWAY
    }
  }
}
