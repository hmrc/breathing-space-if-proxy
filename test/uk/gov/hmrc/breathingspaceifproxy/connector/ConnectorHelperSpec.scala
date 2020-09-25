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

import cats.syntax.option._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.{MimeTypes, Status}
import play.api.libs.json._
import play.api.mvc.Result
import uk.gov.hmrc.breathingspaceifproxy.Header.CorrelationId
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.Error.httpErrorIds
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseSpec, ErrorItem}
import uk.gov.hmrc.http.{HttpException, HttpResponse}

class ConnectorHelperSpec extends AnyWordSpec with BaseSpec with ConnectorHelper {

  implicit lazy val url = Url("http://aHost/aPath")

  "composeResponse" should {
    "return an Http Response with the same data of the Response provided by IF" in {
      val expectedStatus = Status.OK
      val expectedContent = "Some Content"

      Given("a HttpResponse parameter")
      val httpResponse = HttpResponse(
        expectedStatus,
        s"""{ "content" : "$expectedContent" }""",
        Map(
          CONTENT_TYPE -> List(MimeTypes.JSON),
          CorrelationId -> List(correlationIdAsString)
        )
      )

      val result = composeResponse(httpResponse)
      val bodyAsJson = verifyResult(result, expectedStatus)
      (bodyAsJson \ "content").as[String] shouldBe expectedContent
    }
  }

  "logException" should {

    "return an Http Response reporting the HttpException caught while calling IF" in {
      val expectedStatus = Status.INTERNAL_SERVER_ERROR
      val expectedMessage = "Unknown Nino"

      Given("a caught HttpException")
      val httpException = new HttpException(expectedMessage, Status.NOT_ACCEPTABLE)

      val result = logException.apply(httpException).futureValue
      val bodyAsJson = verifyResult(result, expectedStatus)

      And("the body should contain an \"errors\" list with 1 detail error")
      val errorList = (bodyAsJson \ "errors").as[List[ErrorItem]]
      errorList.size shouldBe 1
      errorList.head.code shouldBe httpErrorIds.get(Status.NOT_ACCEPTABLE).head
      errorList.head.message shouldBe expectedMessage
    }

    "return an Http Response reporting any other Throwable caught while calling IF" in {
      val expectedStatus = Status.INTERNAL_SERVER_ERROR
      val expectedMessage = "Some illegal argument"

      Given("a caught Throwable")
      val throwable = new IllegalArgumentException(expectedMessage)

      val result = logException.apply(throwable).futureValue
      val bodyAsJson = verifyResult(result, expectedStatus)

      And("the body should contain an \"errors\" list with 1 detail error")
      val errorList = (bodyAsJson \ "errors").as[List[ErrorItem]]
      errorList.size shouldBe 1
      errorList.head.code shouldBe httpErrorIds.get(Status.INTERNAL_SERVER_ERROR).head
      errorList.head.message shouldBe expectedMessage
    }
  }

  def verifyResult(result: Result, expectedStatus: Int): JsValue = {
    Then(s"the resulting Response should have as Http Status $expectedStatus")
    val responseHeader = result.header
    responseHeader.status shouldBe expectedStatus

    And("a body in Json format")
    val headers = responseHeader.headers
    headers.size shouldBe 2
    headers.get(CONTENT_TYPE) shouldBe Some(MimeTypes.JSON)

    And("a 'Correlation-Id' header")
    headers.get(CorrelationId) shouldBe correlationIdAsString.some

    And("the expected Body")
    result.body.contentType shouldBe Some(MimeTypes.JSON)
    val value = result.body.consumeData.futureValue.utf8String
    Json.parse(value)
  }
}
