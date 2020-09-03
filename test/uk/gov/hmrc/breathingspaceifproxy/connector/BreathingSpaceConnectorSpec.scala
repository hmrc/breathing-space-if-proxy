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

package uk.gov.hmrc.breathingspaceifproxy.connector

import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.HeaderCorrelationId
import uk.gov.hmrc.breathingspaceifproxy.model.ErrorResponse.correlationIdName
import uk.gov.hmrc.breathingspaceifproxy.model.Url
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.{HttpException, HttpResponse}

class BreathingSpaceConnectorSpec extends BaseSpec with BreathingSpaceConnectorHelper {

  implicit lazy val url = Url("http://aHost/aPath")

  "composeResponseFromIF" should {
    "return an Http Response with the same data of the Response provided by IF" in {
      val expectedStatus = Status.OK
      val expectedContent = "Some Content"

      Given("a HttpResponse parameter")
      val httpResponse = HttpResponse(
        expectedStatus,
        s"""{"content" : "$expectedContent"}""",
        Map(
          CONTENT_TYPE -> List(MimeTypes.JSON),
          HeaderCorrelationId -> List(correlationId)
        )
      )

      val result = composeResponseFromIF(httpResponse)
      val bodyAsJson = verifyResult(result, expectedStatus)
      (bodyAsJson \ "content").as[String] shouldBe expectedContent
    }
  }

  "logException" should {

    "return an Http Response reporting the HttpException caught while calling IF" in {
      val expectedStatus = Status.NOT_FOUND
      val expectedReason = "Unknown Nino"

      Given("a caught HttpException")
      val httpException = new HttpException(expectedReason, Status.NOT_FOUND)

      val result = logException.apply(httpException).futureValue
      val bodyAsJson = verifyResult(result, expectedStatus)
      (bodyAsJson \ correlationIdName).as[String] shouldBe correlationId
      (bodyAsJson \ "reason").as[String] shouldBe expectedReason
    }

    "return an Http Response reporting any other Throwable caught while calling IF" in {
      val expectedStatus = Status.INTERNAL_SERVER_ERROR
      val expectedReason = "Some illegal argument"

      Given("a caught Throwable")
      val throwable = new IllegalArgumentException(expectedReason)

      val result = logException.apply(throwable).futureValue
      val bodyAsJson = verifyResult(result, expectedStatus)
      (bodyAsJson \ correlationIdName).as[String] shouldBe correlationId
      (bodyAsJson \ "reason").as[String] shouldBe expectedReason
    }
  }

  def verifyResult(result: Result, expectedStatus: Int): JsValue = {
    Then(s"the resulting Response should have as Http Status $expectedStatus")
    val responseHeader = result.header
    responseHeader.status shouldBe expectedStatus

    And("same Headers")
    val headers = responseHeader.headers
    headers.size shouldBe 2
    headers.get(CONTENT_TYPE) shouldBe Some(MimeTypes.JSON)
    headers.get(HeaderCorrelationId) shouldBe Some(correlationId)

    And("same Body")
    result.body.contentType shouldBe Some(MimeTypes.JSON)
    val value = result.body.consumeData.futureValue.utf8String
    Json.parse(value)
  }
}
