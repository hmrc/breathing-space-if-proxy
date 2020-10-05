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

package uk.gov.hmrc.breathingspaceifproxy.config

import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.SERVER_ERROR
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class ErrorHandlerSpec extends AnyWordSpec with BaseSpec {

  "onClientError" should {
    "return an error message as response's body according to the expected format (a list of errors)" in {
      val statusCode = BAD_REQUEST
      val expectedMessage = "Invalid Json."
      val request = requestFilteredOutOneHeader(Header.CorrelationId)

      val response = inject[ErrorHandler].onClientError(request, statusCode, expectedMessage)

      val expectedBody = s"""{"errors":[{"code":"BAD_REQUEST","message":"$expectedMessage"}]}"""

      status(response) shouldBe BAD_REQUEST
      contentAsString(response) shouldBe expectedBody
    }

    "return a error message according to the expected format but without code detail, if any" in {
      val statusCode = BAD_REQUEST

      val expectedMessage = "Invalid Json."
      val codeDetailIfAny = "At Source[ akka.stream..."
      val message = s"${expectedMessage}\n${codeDetailIfAny}"

      val response = inject[ErrorHandler].onClientError(fakeGetRequest, statusCode, message)

      val expectedBody = s"""{"errors":[{"code":"BAD_REQUEST","message":"$expectedMessage"}]}"""

      status(response) shouldBe BAD_REQUEST
      contentAsString(response) shouldBe expectedBody
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
