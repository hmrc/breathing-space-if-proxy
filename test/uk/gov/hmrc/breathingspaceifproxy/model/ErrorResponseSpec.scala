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
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.funsuite.AnyFunSuite
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseSpec, ErrorItem}

class ErrorResponseSpec extends AnyFunSuite with BaseSpec {

  test("ErrorResponse with httpErrorCode param and an 'Errors' list with 1 'Error' item") {
    genAndTestErrorResponse(true, Nec(Error(INVALID_NINO)))
  }

  test("ErrorResponse with httpErrorCode param and an 'Errors' list with 2 'Error' items") {
    genAndTestErrorResponse(true, Nec(Error(INVALID_NINO), Error(INVALID_DATE)))
  }

  test("ErrorResponse with an 'Errors' list with 1 'Error' item") {
    genAndTestErrorResponse(false, Nec(Error(SERVER_ERROR)))
  }

  test("ErrorResponse with an 'Errors' list with 2 'Error' items") {
    genAndTestErrorResponse(false, Nec(Error(INVALID_NINO), Error(INVALID_DATE)))
  }

  private def genAndTestErrorResponse(withHttpErrorCode: Boolean, errors: Nec[Error]): Assertion = {
    Given("an Http Status >= 400")
    val httpErrorCode = if (withHttpErrorCode) Status.BAD_REQUEST else errors.head.baseError.httpCode

    val nrErrors = errors.length
    And(s"$nrErrors Error items")

    Then("the resulting ErrorResponse instance should wrap an Http response")
    val response =
      if (withHttpErrorCode) ErrorResponse(correlationIdAsString.some, httpErrorCode, errors).value.futureValue
      else {
        if (nrErrors == 1) {
          // Testing the 'apply' taking a single 'Error'
          testErrorResponse(ErrorResponse(correlationId, errors.head).value.futureValue, httpErrorCode, errors)
        }
        ErrorResponse(correlationId, errors).value.futureValue
      }

    testErrorResponse(response, httpErrorCode, errors)
  }

  private def testErrorResponse(response: Result, httpErrorCode: Int, errors: Nec[Error]): Assertion = {
    And("the Http response should have the Http Status provided")
    response.header.status shouldBe httpErrorCode

    And("a \"Correlation-Id\" header")
    response.header.headers.get(Header.CorrelationId) shouldBe correlationIdAsString.some

    And("a body in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    val nrErrors = errors.length.toInt
    And(s"'errors' should be a list with $nrErrors Error items")
    val errorList = (bodyAsJson \ "errors").as[List[ErrorItem]]
    errorList.size shouldBe nrErrors
    assert(errorList.forall { errorItem =>
      errors.exists { error =>
        errorItem.code == error.baseError.entryName
      }
    })
  }
}
