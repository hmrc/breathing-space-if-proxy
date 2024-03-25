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

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.PeriodsController.get
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.INVALID_BODY
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Periods_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsControllerGetISpec extends BaseISpec {

  val nino = genNino
  val getPathWithValidNino = get(nino.value).url
  val periodsConnectorUrl = PeriodsConnector.path(nino)

  "GET BS Periods for Nino" should {

    "return 200(OK) and all periods for the valid Nino provided" in {
      verifyOk(attended = true)
    }

    "return 200(OK) even for a valid Nino with a trailing blank" in {
      val ninoWithoutSuffix = genNino
      val controllerUrl = get(s"${ninoWithoutSuffix.value} ").url
      val connectorUrl =
        PeriodsConnector
          .path(ninoWithoutSuffix)
          .replace(ninoWithoutSuffix.value, s"${ninoWithoutSuffix.value}%20")

      val expectedBody = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Get, connectorUrl, Status.OK, expectedBody)

      val response = route(app, fakeAttendedRequest(Helpers.GET, controllerUrl)).get

      status(response) shouldBe Status.OK
      contentAsString(response) shouldBe expectedBody

      verifyHeaders(HttpMethod.Get, connectorUrl)
      verifyAuditEventCall(BS_Periods_GET)
      headers(response).get("Cache-Control") shouldBe Some(appConfig.httpHeaderCacheControl)
    }

    "return 200(OK) and an empty list of periods for the valid Nino provided" in {
      val expectedBody = """{"periods":[]}"""
      stubCall(HttpMethod.Get, periodsConnectorUrl, Status.OK, expectedBody)

      val response = route(app, fakeAttendedRequest(Helpers.GET, getPathWithValidNino)).get
      status(response) shouldBe Status.OK
      contentAsString(response) shouldBe expectedBody

      verifyHeaders(HttpMethod.Get, periodsConnectorUrl)
      verifyAuditEventCall(BS_Periods_GET)
      headers(response).get("Cache-Control") shouldBe Some(appConfig.httpHeaderCacheControl)
    }

    "return 200(OK) for an ATTENDED request" in {
      verifyOk(attended = true)
    }

    "return 200(OK) for an UNATTENDED request" in {
      verifyOk(attended = false)
    }

    "return 400(BAD_REQUEST) when a body is provided" in {
      val body = Json.obj("aName" -> "aValue")
      val request = fakeAttendedRequest(Helpers.GET, getPathWithValidNino).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_BODY")
      errorList.head.code shouldBe INVALID_BODY.entryName
      assert(errorList.head.message.startsWith(INVALID_BODY.message))
    }

    "return 401(UNAUTHORIZED) when the request was not authorised" in {
      verifyUnauthorized(fakeAttendedRequest(Helpers.GET, getPathWithValidNino))
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val unknownNino = genNino
      val url = PeriodsConnector.path(unknownNino)
      val errorBody = """"code":"RESOURCE_NOT_FOUND""""
      stubCall(HttpMethod.Get, url, Status.NOT_FOUND, errorResponseFromIF(errorBody))
      val response = route(app, fakeAttendedRequest(Helpers.GET, get(unknownNino.value).url)).get
      status(response) shouldBe Status.NOT_FOUND

      verifyHeaders(HttpMethod.Get, url)
      verifyAuditEventCall(BS_Periods_GET)
      headers(response).get("Cache-Control") shouldBe Some(appConfig.httpHeaderCacheControl)
    }
  }

  private def verifyOk(attended: Boolean): Assertion = {
    val expectedResponseBody = Json.toJson(validPeriodsResponse).toString
    stubCall(HttpMethod.Get, periodsConnectorUrl, Status.OK, expectedResponseBody)

    val request =
      if (attended) fakeAttendedRequest(Helpers.GET, getPathWithValidNino)
      else fakeUnattendedRequest(Helpers.GET, getPathWithValidNino)

    val response = route(app, request).get
    status(response) shouldBe Status.OK
    contentAsString(response) shouldBe expectedResponseBody

    if (attended) verifyHeadersForAttended(HttpMethod.Get, periodsConnectorUrl)
    else verifyHeadersForUnattended(HttpMethod.Get, periodsConnectorUrl)

    verifyAuditEventCall(BS_Periods_GET)
  }
}
