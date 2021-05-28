/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.syntax.option._
import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite
import play.api.mvc.Result
import play.api.test._
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.model.enums.Attended
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class HeadersValidationSpec extends AnyFunSuite with BaseSpec {

  val controller =
    new PeriodsController(
      appConfig,
      inject[AuditConnector],
      authConnector,
      Helpers.stubControllerComponents(),
      inject[PeriodsConnector]
    )

  test(s"Response should be 400(BAD_REQUEST) when the $CorrelationId header is missing") {
    verifyHeaderIsMissing(
      controller.get(genNinoString)(attendedRequestFilteredOutOneHeader(CorrelationId)).run,
      CorrelationId
    )
  }

  test(s"return 400(BAD_REQUEST) when the $RequestType header is missing") {
    verifyHeaderIsMissing(
      controller.get(genNinoString)(attendedRequestFilteredOutOneHeader(RequestType)).run,
      RequestType
    )
  }

  test(s"return 400(BAD_REQUEST) when the $StaffPid header is missing") {
    verifyHeaderIsMissing(controller.get(genNinoString)(attendedRequestFilteredOutOneHeader(StaffPid)).run, StaffPid)
  }

  test("return 400(BAD_REQUEST) for a GET when all required headers are missing") {
    Given("a GET request without any of the requested headers")
    val response = controller.get(genNinoString)(FakeRequest()).run

    val errorList = verifyErrorResult(response, BAD_REQUEST, None, 3)

    And("the error code for all elements should be MISSING_HEADER")
    assert(errorList.forall(_.code == MISSING_HEADER.entryName))
  }

  test("return 400(BAD_REQUEST) for a POST when the Content-Type header is missing") {
    val body = postPeriodsRequestAsJson(postPeriodsRequest())
    val request = unattendedRequestFilteredOutOneHeader(CONTENT_TYPE, "POST").withBody(body)

    verifyHeaderIsMissing(controller.post(request), CONTENT_TYPE)
  }

  test("return 400(BAD_REQUEST) for a POST when all required headers are missing") {
    Given("a POST request without any of the requested headers")
    val body = postPeriodsRequestAsJson(postPeriodsRequest())
    val request = FakeRequest(POST, "/").withBody(body)

    val response = controller.post(request)

    val errorList = verifyErrorResult(response, BAD_REQUEST, None, 4)

    And("the error code for all elements should be MISSING_HEADER")
    assert(errorList.forall(_.code == MISSING_HEADER.entryName))
  }

  test("return 400(BAD_REQUEST) for a 'POST Periods' when the RequestType header is DA2_BS_ATTENDED") {
    Given(s"a 'POST Periods' request with a $RequestType header equal to ${Attended.DA2_BS_ATTENDED}")
    val body = postPeriodsRequestAsJson(postPeriodsRequest())
    val request = attendedRequestWithAllHeaders(POST).withBody(body)

    val response = controller.post(request)

    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    And("the error code for all elements should be INVALID_HEADER")
    assert(errorList.forall(_.code == INVALID_HEADER.entryName))
  }

  test("return 400(BAD_REQUEST) for a PUT when the Content-Type header is missing") {
    val body = putPeriodsRequest(putPeriodsRequest)
    val request = unattendedRequestFilteredOutOneHeader(CONTENT_TYPE, "PUT").withBody(body)

    verifyHeaderIsMissing(controller.put(genNinoString)(request), CONTENT_TYPE)
  }

  test("return 400(BAD_REQUEST) for a 'PUT Periods' when the RequestType header is DA2_BS_ATTENDED") {
    Given(s"a 'PUT Periods' request with a $RequestType header equal to ${Attended.DA2_BS_ATTENDED}")
    val body = putPeriodsRequest(putPeriodsRequest)
    val request = attendedRequestWithAllHeaders(PUT).withBody(body)

    val response = controller.put(genNinoString)(request)

    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    And("the error code for all elements should be INVALID_HEADER")
    assert(errorList.forall(_.code == INVALID_HEADER.entryName))
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
