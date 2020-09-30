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

package uk.gov.hmrc.breathingspaceifproxy.controller

import scala.concurrent.Future

import org.mockito.scalatest.MockitoSugar
import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.Header._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class HeadersValidationSpec extends AnyFunSuite with BaseSpec with MockitoSugar {

  val controller =
    new PeriodsController(appConfig, Helpers.stubControllerComponents(), inject[PeriodsConnector])

  test(s"Response should be 400(BAD_REQUEST) when the $CorrelationId header is missing") {
    verifyHeaderIsMissing(controller.get(validNinoAsString)(requestFilteredOutOneHeader(CorrelationId)), CorrelationId)
  }

  test(s"return 400(BAD_REQUEST) when the $RequestType header is missing") {
    verifyHeaderIsMissing(controller.get(validNinoAsString)(requestFilteredOutOneHeader(RequestType)), RequestType)
  }

  test(s"return 400(BAD_REQUEST) when the $StaffPid header is missing") {
    verifyHeaderIsMissing(controller.get(validNinoAsString)(requestFilteredOutOneHeader(StaffPid)), StaffPid)
  }

  test("return 400(BAD_REQUEST) for a GET when all required headers are missing") {
    Given("a GET request without any of the requested headers")
    val response = controller.get(validNinoAsString)(FakeRequest())

    val errorList = verifyErrorResult(response, BAD_REQUEST, None, 3)

    And("the error code for all elements should be MISSING_HEADER")
    assert(errorList.forall(_.code == MISSING_HEADER.entryName))
  }

  test(s"return 400(BAD_REQUEST) for a POST when the $CONTENT_TYPE header is missing") {
    val request = requestFilteredOutOneHeader(CONTENT_TYPE, POST)
      .withJsonBody(createPeriodsRequest(validPeriods))
    verifyHeaderIsMissing(controller.post()(request), CONTENT_TYPE)
  }

  test("return 400(BAD_REQUEST) for a POST when all required headers are missing") {
    Given("a POST request without any of the requested headers")
    val request = FakeRequest(POST, "/").withJsonBody(createPeriodsRequest(validPeriods))
    val response = controller.post()(request)

    val errorList = verifyErrorResult(response, BAD_REQUEST, None, 4)

    And("the error code for all elements should be MISSING_HEADER")
    assert(errorList.forall(_.code == MISSING_HEADER.entryName))
  }

  private def verifyHeaderIsMissing(f: => Future[Result], headerFilteredOut: String): Assertion = {
    Given(s"a request without the $headerFilteredOut request header")
    val response: Future[Result] = f

    val errorList = verifyErrorResult(
      response,
      BAD_REQUEST,
      correlationIdAsOpt(headerFilteredOut != CorrelationId),
      1
    )

    And(s"the error code should be ${MISSING_HEADER.entryName}")
    errorList.head.code shouldBe MISSING_HEADER.entryName
    assert(errorList.head.message.contains(headerFilteredOut))
  }
}
