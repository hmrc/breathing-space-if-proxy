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

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.PeriodsController.post
import uk.gov.hmrc.breathingspaceifproxy.model.PostPeriodsInRequest
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Periods_POST
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsControllerPostISpec extends BaseISpec {

  val postPath = post.url

  "POST BS Periods for Nino" should {

    "return 201(CREATED) and all periods for the valid Nino provided" in {
      verifyCreated()
    }

    "return 201(CREATED) even when the UTR is not provided" in {
      verifyCreated(postPeriodsRequest(none))
    }

    "return 400(BAD_REQUEST) when no body is provided" in {
      val request = fakeUnattendedRequest(Helpers.POST, postPath)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_BODY")
      errorList.head.code shouldBe MISSING_BODY.entryName
      assert(errorList.head.message.startsWith(MISSING_BODY.message))
    }

    "return 400(BAD_REQUEST) when body is not valid Json" in {
      val body = s"""{nino":"${genNinoString}","periods":[${Json.toJson(validPostPeriod).toString}]}"""
      val request = fakeUnattendedRequest(Helpers.POST, postPath).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_JSON")
      errorList.head.code shouldBe INVALID_JSON.entryName
      assert(errorList.head.message.startsWith(INVALID_JSON.message))
    }

    "return 401(UNAUTHORIZED) when the request was not authorised" in {
      val body = postPeriodsRequestAsJson(genNino.value, postPeriodsRequest())
      val request = fakeUnattendedRequest(Helpers.POST, postPath).withBody(body)
      verifyUnauthorized(request)
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val unknownNino = genNino
      val url = PeriodsConnector.path(unknownNino)
      val errorBody = """"code":"RESOURCE_NOT_FOUND""""
      stubCall(HttpMethod.Post, url, Status.NOT_FOUND, errorResponseFromIF(errorBody))

      val body = postPeriodsRequestAsJson(unknownNino.value, postPeriodsRequest())
      val request = fakeUnattendedRequest(Helpers.POST, postPath).withBody(body)

      val response = route(app, request).get
      status(response) shouldBe Status.NOT_FOUND

      verifyHeadersForUnattended(HttpMethod.Post, url)
      verifyAuditEventCall(BS_Periods_POST)
      headers(response).get("Cache-Control") shouldBe Some(appConfig.httpHeaderCacheControl)
    }
  }

  private def verifyCreated(postPeriods: PostPeriodsInRequest = postPeriodsRequest()): Assertion = {
    val nino = genNino
    val url = PeriodsConnector.path(nino)
    val expectedResponseBody = Json.toJson(validPeriodsResponse).toString
    stubCall(HttpMethod.Post, url, Status.CREATED, expectedResponseBody)

    val body = postPeriodsRequestAsJson(nino.value, postPeriods)
    val request = fakeUnattendedRequest(Helpers.POST, postPath).withBody(body)

    val response = route(app, request).get
    status(response) shouldBe Status.CREATED
    contentAsString(response) shouldBe expectedResponseBody

    verifyHeadersForUnattended(HttpMethod.Post, url)
    verifyAuditEventCall(BS_Periods_POST)
    headers(response).get("Cache-Control") shouldBe Some(appConfig.httpHeaderCacheControl)
  }
}
