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

package uk.gov.hmrc.breathingspaceifproxy.config

import cats.syntax.option._
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class ErrorHandlerSpec extends AnyWordSpec with BaseSpec {

  val errorHandler = inject[ErrorHandler]

  "onClientError" should {
    "return an error message as response's body according to the expected format (a list of errors)" in {
      val statusCode = BAD_REQUEST
      val expectedMessage = "Invalid Json."
      val request = attendedRequestFilteredOutOneHeader(DownstreamHeader.CorrelationId)

      val response = errorHandler.onClientError(request, statusCode, expectedMessage)

      val expectedBody = s"""{"errors":[{"code":"BAD_REQUEST","message":"$expectedMessage"}]}"""

      status(response) shouldBe statusCode
      contentAsString(response) shouldBe expectedBody
    }

    "return a error message according to the expected format but without code detail, if any" in {
      val statusCode = BAD_REQUEST

      val expectedMessage = "Invalid Json."
      val codeDetailIfAny = "At Source[ akka.stream..."
      val message = s"${expectedMessage}\n${codeDetailIfAny}"

      val response = inject[ErrorHandler].onClientError(fakeGetRequest, statusCode, message)

      val expectedBody = s"""{"errors":[{"code":"BAD_REQUEST","message":"$expectedMessage"}]}"""

      status(response) shouldBe statusCode
      contentAsString(response) shouldBe expectedBody
    }

    "return 400(BAD_REQUEST) and INVALID_ENDPOINT as code when receiving a 404(NOT_FOUND)" in {
      val response = errorHandler.onClientError(fakeGetRequest, NOT_FOUND, "Invalid endpoint")

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_ENDPOINT")
      errorList.head.code shouldBe INVALID_ENDPOINT.entryName
      assert(errorList.head.message.startsWith(INVALID_ENDPOINT.message))
    }
  }

  "onServerError" should {
    "return an error message as response's body according to the expected format (a list of errors)" in {
      val response = inject[ErrorHandler].onServerError(fakeGetRequest, new NoSuchElementException())

      status(response) shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(response) \ "errors" \\ "code").head.as[String] shouldBe SERVER_ERROR.entryName
    }
  }
}
