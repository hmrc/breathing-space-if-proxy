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

package uk.gov.hmrc.breathingspaceifproxy.model

import cats.data.{NonEmptyChain => Nec}
import cats.syntax.option._
import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseSpec, TestingErrorItem}

class HttpErrorSpec extends AnyFunSuite with BaseSpec {

  test("HttpError with httpErrorCode param and an 'Errors' list with 1 'Error' item") {
    genAndTestHttpError(true, Nec(ErrorItem(INVALID_NINO)))
  }

  test("HttpError with httpErrorCode param and an 'Errors' list with 2 'Error' items") {
    genAndTestHttpError(true, Nec(ErrorItem(INVALID_NINO), ErrorItem(INVALID_JSON_ITEM)))
  }

  test("HttpError with an 'Errors' list with 1 'Error' item") {
    genAndTestHttpError(false, Nec(ErrorItem(INTERNAL_SERVER_ERROR)))
  }

  test("HttpError with an 'Errors' list with 2 'Error' items") {
    genAndTestHttpError(false, Nec(ErrorItem(INVALID_NINO), ErrorItem(INVALID_JSON_ITEM)))
  }

  private def genAndTestHttpError(withHttpErrorCode: Boolean, errorItems: Nec[ErrorItem]): Assertion = {
    Given("an Http Status >= 400")
    val httpErrorCode = if (withHttpErrorCode) Status.BAD_REQUEST else errorItems.head.baseError.httpCode

    val nrErrors = errorItems.length
    And(s"$nrErrors Error items")

    Then("the resulting HttpError instance should wrap an Http response")
    val response =
      if (withHttpErrorCode) HttpError(correlationIdAsString.some, httpErrorCode, errorItems).value
      else {
        if (nrErrors == 1) {
          // Testing the 'apply' taking a single 'Error'
          testHttpError(HttpError(correlationIdAsString.some, errorItems.head).value, httpErrorCode, errorItems)
        }
        HttpError(correlationId, errorItems).value
      }

    testHttpError(response, httpErrorCode, errorItems)
  }

  private def testHttpError(response: Result, httpErrorCode: Int, errorItems: Nec[ErrorItem]): Assertion = {
    And("the Http response should have the Http Status provided")
    response.header.status shouldBe httpErrorCode

    And("a \"Correlation-Id\" header")
    response.header.headers.get(DownstreamHeader.CorrelationId) shouldBe correlationIdAsString.some

    And("a body in Json format")
    response.header.headers.get(CONTENT_TYPE) shouldBe Option(MimeTypes.JSON)
    response.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(response.body.consumeData.futureValue.utf8String)

    val nrErrors = errorItems.length.toInt
    And(s"'errors' should be a list with $nrErrors Error items")
    val errorList = (bodyAsJson \ "errors").as[List[TestingErrorItem]]
    errorList.size shouldBe nrErrors
    assert(errorList.forall { errorItem =>
      errorItems.exists { error =>
        errorItem.code == error.baseError.entryName
      }
    })
  }
}
