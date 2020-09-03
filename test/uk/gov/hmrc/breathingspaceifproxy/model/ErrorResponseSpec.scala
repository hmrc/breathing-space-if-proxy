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

import java.util.UUID

import akka.stream.Materializer
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.model.ErrorResponse.correlationIdName
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpException}

class ErrorResponseSpec
    extends AnyFunSuite
    with DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneAppPerSuite
    with HttpErrorFunctions
    with Injecting
    with Matchers {

  implicit lazy val materializer: Materializer = inject[Materializer]

  lazy val url = Url("http://aHost/aPath")

  test("ErrorResponse for Http errors (with 2 parameters)") {
    Given("an Http Status >= 300 as 1st parameter")
    val errorCode = Status.BAD_REQUEST

    And("an error message as 2nd parameter")
    val expectedReason = "Some required header are missing"

    Then("the resulting ErrorResponse instance should wrap an Http response")
    val response = ErrorResponse(errorCode, expectedReason, None).value.futureValue

    And("the Http response should have the Http Status provided")
    response.header.status shouldBe Status.BAD_REQUEST

    And("a content in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    And("the content should include the error message provided")
    (bodyAsJson \ "reason").as[String] shouldBe expectedReason

    And("not include a correlation-id property")
    (bodyAsJson \ correlationIdName).asOpt[String] shouldBe None
  }

  test("ErrorResponse for Http errors (with 3 parameters)") {
    Given("an Http Status >= 300 as 1st parameter")
    val errorCode = Status.BAD_REQUEST

    And("an error message as 2nd parameter")
    val expectedReason = "Some required header are missing"

    And("a correlationId as 3rd parameter")
    val correlationId = UUID.randomUUID.toString

    Then("the resulting ErrorResponse instance should wrap an Http response")
    val response = ErrorResponse(errorCode, expectedReason, Some(correlationId)).value.futureValue

    And("the Http response should have the Http Status provided")
    response.header.status shouldBe Status.BAD_REQUEST

    And("a content in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    And("the content should include the error message provided")
    (bodyAsJson \ "reason").as[String] shouldBe expectedReason

    And("the correlationId provided")
    (bodyAsJson \ correlationIdName).as[String] shouldBe correlationId
  }

  test("ErrorResponse for caught exceptions (with 3 parameters)") {
    Given("a caught HttpException")
    val expectedStatus = Status.NOT_FOUND
    val expectedReason = "Unknown Nino"
    val httpException = new HttpException(expectedReason, expectedStatus)

    Then("the resulting ErrorResponse instance should wrap an Http response")
    val reasonToLog = s"HTTP error(${expectedStatus}) while calling ${url.value}."
    val response = ErrorResponse(expectedStatus, reasonToLog, httpException, None).value.futureValue

    And("the Http response should have the Http Status provided")
    response.header.status shouldBe expectedStatus

    And("a content in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    And("the content should include the error message provided")
    (bodyAsJson \ "reason").as[String] shouldBe expectedReason

    And("not include a correlation-id property")
    (bodyAsJson \ correlationIdName).asOpt[String] shouldBe None
  }

  test("ErrorResponse for caught exceptions (with 4 parameters)") {
    Given("a caught throwable")
    val expectedReason = "Some illegal argument"
    val throwable = new IllegalArgumentException(expectedReason)

    And("a correlationId as 4thd parameter")
    val correlationId = UUID.randomUUID.toString

    Then("the resulting ErrorResponse instance should wrap an Http response")
    val expectedStatus = Status.INTERNAL_SERVER_ERROR
    val reasonToLog = "Some exception was caught.."
    val response = ErrorResponse(expectedStatus, reasonToLog, throwable, Some(correlationId)).value.futureValue

    And("the Http response should have the Http Status provided")
    response.header.status shouldBe expectedStatus

    And("a content in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    And("the content should include the error message provided")
    (bodyAsJson \ "reason").as[String] shouldBe expectedReason

    And("the correlationId provided")
    (bodyAsJson \ correlationIdName).as[String] shouldBe correlationId
  }
}
