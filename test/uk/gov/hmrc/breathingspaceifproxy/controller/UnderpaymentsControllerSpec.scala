/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.implicits.{catsSyntaxOptionId, catsSyntaxValidatedIdBinCompat0}
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsSuccess
import play.api.test.Helpers
import play.api.test.Helpers.*
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.connector.UnderpaymentsConnector
import uk.gov.hmrc.breathingspaceifproxy.connector.service.EisConnector
import uk.gov.hmrc.breathingspaceifproxy.model.*
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.*
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class UnderpaymentsControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockUpstreamConnector: EisConnector = mock[EisConnector]
  when(mockUpstreamConnector.currentState).thenReturn("HEALTHY")

  val mockUnderpaymentsConnector: UnderpaymentsConnector = mock[UnderpaymentsConnector]
  when(mockUnderpaymentsConnector.eisConnector).thenReturn(mockUpstreamConnector)

  val subject = new UnderpaymentsController(
    appConfig,
    inject[AuditConnector],
    authConnector,
    Helpers.stubControllerComponents(),
    mockUnderpaymentsConnector
  )

  val u1: Underpayment = Underpayment("2011", 123.12, "PAYE UP")
  val u2: Underpayment = Underpayment("2011", 123.12, "PAYE UP")
  val u3: Underpayment = Underpayment("2011", 123.12, "PAYE UP")

  val underpayments: List[Underpayment] = List(u1, u2, u3)
  val validPeriodId: String             = UUID.randomUUID().toString
  val validNino                         = "AS000001A"
  val invalidPeriodId                   = "abcdefg"
  val nonExistentNino                   = "AS000001A"
  val nonExistentPeriodId: String       = UUID.randomUUID().toString

  "get" should {
    "return status code 200 when nino and periodId are valid" in {
      Given("valid GET request -> 200 response code")
      when(mockUnderpaymentsConnector.get(any[Nino], any[UUID])(any[RequestId]))
        .thenReturn(Future.successful(Underpayments(underpayments).validNec))

      val response = subject.get(validNino, validPeriodId)(fakeUnAttendedGetRequest)

      status(response) shouldBe OK
    }

    "return 3 underpayments" in {
      Given("valid GET request -> 3 Underpayments payload")
      when(mockUnderpaymentsConnector.get(any[Nino], any[UUID])(any[RequestId]))
        .thenReturn(Future.successful(Underpayments(underpayments).validNec))

      val response = subject.get(validNino, validPeriodId)(fakeUnAttendedGetRequest)

      val content             = contentAsJson(response)
      val actualUnderpayments = content.validate[Underpayments] match {
        case JsSuccess(values, _) => values
        case _                    => fail("message could not be parsed")
      }
      actualUnderpayments shouldBe Underpayments(underpayments)
    }

    "return 503(SERVICE_UNAVAILABLE) when upstream is unavailable" in {
      Given(s"unavailable upstream service")

      when(mockUnderpaymentsConnector.get(any[Nino], any[UUID])(any[RequestId]))
        .thenReturn(Future.successful(ErrorItem(SERVER_ERROR).invalidNec))

      val resp = subject.get(validNino, validPeriodId)(fakeUnAttendedGetRequest).run()

      val errorList = verifyErrorResult(resp, SERVICE_UNAVAILABLE, correlationIdAsString.some, 1)

      errorList.head.code shouldBe SERVER_ERROR.entryName
      assert(errorList.head.message.startsWith(SERVER_ERROR.message))
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      Given(s"invalid GET request -> 400 bad nino")

      val response = subject.get(invalidNino, validPeriodId)(fakeUnAttendedGetRequest).run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)
      And(s"the error code should be $INVALID_NINO")
      errorList.head.code shouldBe INVALID_NINO.entryName
      assert(errorList.head.message.startsWith(INVALID_NINO.message))
    }

    "return 400(BAD_REQUEST) when the Request type is ATTENDED" in {
      Given(s"invalid GET request -> 400 invalid request type")

      val response = subject.get(validNino, validPeriodId)(fakeGetRequest).run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)
      And(s"the error code should be $INVALID_HEADER")
      errorList.head.code shouldBe INVALID_HEADER.entryName
      assert(errorList.head.message.startsWith(INVALID_HEADER.message))
    }

    "return 400(BAD_REQUEST) when the Period ID is invalid" in {
      Given(s"invalid GET request -> 400 bad Period ID")

      val response = subject.get(validNino, invalidPeriodId)(fakeUnAttendedGetRequest).run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)
      And(s"the error code should be $INVALID_PERIOD_ID")
      errorList.head.code shouldBe INVALID_PERIOD_ID.entryName
      assert(errorList.head.message.startsWith(INVALID_PERIOD_ID.message))
    }

    "return 404(RESOURCE_NOT_FOUND) when nino doesnt exist" in {
      Given(s"valid GET request -> nino doesnt exist")
      when(mockUnderpaymentsConnector.get(any[Nino], any[UUID])(any[RequestId]))
        .thenReturn(Future.successful(ErrorItem(RESOURCE_NOT_FOUND).invalidNec))

      // Because these parameters are valid they get through validation, we receive a 404
      val response = subject.get(nonExistentNino, validPeriodId)(fakeUnAttendedGetRequest).run()

      val result = Await.result(response, Duration(1, TimeUnit.SECONDS))
      result.header.status shouldBe 404
    }

    "return 404(RESOURCE_NOT_FOUND) when periodId doesnt exist" in {
      Given(s"valid GET request -> periodId doesnt exist")
      when(mockUnderpaymentsConnector.get(any[Nino], any[UUID])(any[RequestId]))
        .thenReturn(Future.successful(ErrorItem(RESOURCE_NOT_FOUND).invalidNec))

      // Because these parameters are valid they get through validation, we receive a 404
      val response = subject.get(validNino, nonExistentPeriodId)(fakeUnAttendedGetRequest).run()

      val result = Await.result(response, Duration(1, TimeUnit.SECONDS))
      result.header.status shouldBe 404
    }

    "return 400(BAD_REQUEST) with multiple errors when the Nino is invalid and one required header is missing" in {
      Given(s"a GET request with an invalid Nino and without the ${DownstreamHeader.StaffPid} request header")
      val response =
        subject.get("HT1234B", validPeriodId)(unattendedRequestFilteredOutOneHeader(DownstreamHeader.StaffPid)).run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 2)

      And(s"the 1st error code should be $MISSING_HEADER")
      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.startsWith(MISSING_HEADER.message))

      And(s"the 2nd error code should be $INVALID_NINO")
      errorList.last.code shouldBe INVALID_NINO.entryName
      assert(errorList.last.message.startsWith(INVALID_NINO.message))
    }
  }
}
