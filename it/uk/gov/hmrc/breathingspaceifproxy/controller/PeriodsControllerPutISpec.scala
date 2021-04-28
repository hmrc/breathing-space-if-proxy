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

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.PeriodsController.put
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.{INVALID_JSON, MISSING_BODY}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Periods_PUT
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsControllerPutISpec extends BaseISpec {

  val putPath = put(genNinoString).url

  "PUT BS Periods for Nino" should {

    "return 200(OK) and all periods for the valid Nino provided" in {
      verifyOk
    }

    "return 200(OK) even for a valid Nino with a trailing blank" in {
      val ninoWithoutSuffix = genNino
      val controllerUrl = put(s"${ninoWithoutSuffix.value} ").url
      val connectorUrl =
        PeriodsConnector
          .path(ninoWithoutSuffix)
          .replace(ninoWithoutSuffix.value, s"${ninoWithoutSuffix.value}%20")

      val expectedBody = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Put, connectorUrl, Status.OK, expectedBody)

      val request = fakeUnattendedRequest(Helpers.PUT, controllerUrl).withBody(putPeriodsRequestAsJson(putPeriodsRequest))
      val response = route(app, request).get

      status(response) shouldBe Status.OK
      contentAsString(response) shouldBe expectedBody

      verifyHeadersForUnattended(HttpMethod.Put, connectorUrl)
      verifyAuditEventCall(BS_Periods_PUT)
    }

    "return 400(BAD_REQUEST) when no body is provided" in {
      val request = fakeUnattendedRequest(Helpers.PUT, putPath)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_BODY")
      errorList.head.code shouldBe MISSING_BODY.entryName
      assert(errorList.head.message.startsWith(MISSING_BODY.message))
    }

    "return 400(BAD_REQUEST) when body is not valid Json" in {
      val body = s"""{periods":[${Json.toJson(validPutPeriod).toString}]}"""
      val request = fakeUnattendedRequest(Helpers.PUT, putPath).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_JSON")
      errorList.head.code shouldBe INVALID_JSON.entryName
      assert(errorList.head.message.startsWith(INVALID_JSON.message))
    }

    "return 401(UNAUTHORIZED) when the request was not authorised" in {
      val body = putPeriodsRequestAsJson(putPeriodsRequest)
      val request = fakeUnattendedRequest(Helpers.PUT, put(genNino.value).url).withBody(body)
      verifyUnauthorized(request)
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val unknownNino = genNino
      val connectorUrl = PeriodsConnector.path(unknownNino)
      stubCall(HttpMethod.Put, connectorUrl, Status.NOT_FOUND, errorResponseFromIF())

      val controllerUrl = put(unknownNino.value).url
      val request = fakeUnattendedRequest(Helpers.PUT, controllerUrl)
        .withBody(putPeriodsRequestAsJson(putPeriodsRequest))

      val response = route(app, request).get
      status(response) shouldBe Status.NOT_FOUND

      verifyHeadersForUnattended(HttpMethod.Put, connectorUrl)
      verifyAuditEventCall(BS_Periods_PUT)
    }
  }

  private def verifyOk: Assertion = {
    val nino = genNino
    val connectorUrl = PeriodsConnector.path(nino)
    val expectedResponseBody = Json.toJson(validPeriodsResponse).toString
    stubCall(HttpMethod.Put, connectorUrl, Status.OK, expectedResponseBody)

    val controllerUrl = put(nino.value).url

    val request = fakeUnattendedRequest(Helpers.PUT, controllerUrl).withBody(putPeriodsRequestAsJson(putPeriodsRequest))

    val response = route(app, request).get
    status(response) shouldBe Status.OK
    contentAsString(response) shouldBe expectedResponseBody

    verifyHeadersForUnattended(HttpMethod.Put, connectorUrl)
    verifyAuditEventCall(BS_Periods_PUT)
  }
}
