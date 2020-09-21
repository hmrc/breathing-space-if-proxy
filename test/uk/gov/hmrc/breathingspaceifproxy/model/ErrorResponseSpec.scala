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

package uk.gov.hmrc.breathingspaceifproxy.model

import cats.data.{NonEmptyChain => Nec}
import cats.syntax.option._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.funsuite.AnyFunSuite
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.Error.httpErrorIds
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseSpec, ErrorItem}
import uk.gov.hmrc.http.HttpException

class ErrorResponseSpec extends AnyFunSuite with BaseSpec {

  test("ErrorResponse for Http errors (with 1 detail error)") {
    Given("an Http Status >= 400")
    val httpErrorCode = Status.BAD_REQUEST

    And("one detail error")
    val errors = Nec(Error(INVALID_NINO))

    Then("the resulting ErrorResponse instance should wrap an Http response")
    val response = ErrorResponse(correlationId.value.some, httpErrorCode, errors).value.futureValue

    And("the Http response should have the Http Status provided")
    response.header.status shouldBe httpErrorCode

    And("a \"Correlation-Id\" header")
    response.header.headers.get(Header.CorrelationId) shouldBe Some(correlationId.value)

    And("a body in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    And("\"errors\" should be a list with 1 detail error")
    val errorList = (bodyAsJson \ "errors").as[List[ErrorItem]]
    errorList.size shouldBe 1
    errorList.head.code shouldBe INVALID_NINO.entryName
    errorList.head.message shouldBe INVALID_NINO.message
  }

  test("ErrorResponse for Http errors (with 2 detail errors)") {
    Given("an Http Status >= 400")
    val httpErrorCode = Status.BAD_REQUEST

    And("two detail errors")
    val detail = ". The date not in the expected format"
    val errors = Nec(Error(INVALID_NINO), Error(INVALID_DATE, detail.some))

    Then("the resulting ErrorResponse instance should wrap an Http response")
    val response = ErrorResponse(None, httpErrorCode, errors).value.futureValue

    And("the Http response should have the Http Status provided")
    response.header.status shouldBe httpErrorCode

    And("not a \"Correlation-Id\" header")
    response.header.headers.get(Header.CorrelationId) shouldBe None

    And("a body in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    And("\"errors\" should be a list with 2 detail errors")
    val errorList = (bodyAsJson \ "errors").as[List[ErrorItem]]
    errorList.size shouldBe 2
    errorList.head.code shouldBe INVALID_NINO.entryName
    errorList.head.message shouldBe INVALID_NINO.message
    errorList.last.code shouldBe INVALID_DATE.entryName
    errorList.last.message shouldBe s"${INVALID_DATE.message}$detail"
  }

  test("ErrorResponse for caught exceptions") {
    Given("a caught HttpException")
    val expectedStatus = Status.GATEWAY_TIMEOUT
    val expectedMessage = RESOURCE_NOT_FOUND.message
    val httpException = new HttpException(expectedMessage, expectedStatus)

    Then("the resulting ErrorResponse instance should wrap an Http response")
    val reasonToLog = s"HTTP error(${expectedStatus}) while calling ${Url("http://aHost/aPath").value}."
    val response = ErrorResponse(None, expectedStatus, reasonToLog, httpException).value.futureValue

    And("the Http response should have the Http Status provided")
    response.header.status shouldBe Status.INTERNAL_SERVER_ERROR

    And("a body in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    And("\"errors\" should be a list with 1 detail error")
    val errorList = (bodyAsJson \ "errors").as[List[ErrorItem]]
    errorList.size shouldBe 1
    errorList.head.code shouldBe httpErrorIds.get(Status.GATEWAY_TIMEOUT).head
    errorList.head.message shouldBe expectedMessage
  }
}
