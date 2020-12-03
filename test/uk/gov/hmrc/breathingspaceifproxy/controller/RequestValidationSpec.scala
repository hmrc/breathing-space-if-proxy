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

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.{Attended, BaseError}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.{BS_Details_GET, BS_Periods_POST}
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class RequestValidationSpec extends AnyWordSpec with BaseSpec with RequestValidation {

  "RequestValidation.validateNino" should {
    "assert that an empty Nino value is invalid" in {
      assert(validateNino("").isInvalid)
    }

    "assert that an invalid Nino value is invalid" in {
      assert(validateNino("werr").isInvalid)
    }

    "assert that a valid Nino value, without suffix, is valid" in {
      assert(validateNino(genNinoString).isValid)
    }

    "assert that a valid Nino value, with suffix, is valid" in {
      assert(validateNino(genNinoWithSuffix.value).isValid)
    }

    "assert that a valid Nino value, with a blank in suffix position, is valid" in {
      assert(validateNino(s"${genNino.value} ").isValid)
    }
  }

  "RequestValidation.validateCorrelationId" should {
    "capture missing CorrelationId header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.CorrelationId)

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, Header.CorrelationId)
    }

    "capture empty value for the CorrelationId header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.CorrelationId, ""))

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.CorrelationId)
    }

    "capture invalid value for the CorrelationId header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.CorrelationId, "334534534534"))

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.CorrelationId)
    }

    "assert a valid CorrelationId value is valid" in {
      assert(validateHeadersForNPS(BS_Details_GET)(fakeGetRequest).isValid)
    }
  }

  "RequestValidation.validateRequestType" should {
    "capture missing RequestType header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.RequestType)

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, Header.RequestType)
    }

    "capture empty value for the RequestType header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.RequestType, ""))

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.RequestType)
    }

    "capture invalid value for the RequestType header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.RequestType, "334534534534"))

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.RequestType)
    }

    "assert a RequestType value of ATTENDED is valid" in {
      assert(validateHeadersForNPS(BS_Details_GET)(fakeGetRequest).isValid)
    }

    "assert a RequestType value of UNATTENDED is valid" in {
      val mockRequest = fakeGetRequest.withHeaders(
        (Header.RequestType, Attended.DA2_BS_UNATTENDED.toString),
        (Header.StaffPid, unattendedStaffPid)
      )

      assert(validateHeadersForNPS(BS_Details_GET)(mockRequest).isValid)
    }
  }

  // TODO: Check that this is even possible on API Platform
  "RequestValidation.validateContentType" should {
    "capture missing ContentType header for non-GET methods" in {
      val mockRequest = requestFilteredOutOneHeader(CONTENT_TYPE, "POST")

      val result = validateHeadersForNPS(BS_Periods_POST)(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, CONTENT_TYPE)
    }

    "allow missing ContentType header for GET methods" in {
      val mockRequest = requestFilteredOutOneHeader(CONTENT_TYPE)

      assert(validateHeadersForNPS(BS_Details_GET)(mockRequest).isValid)
    }

    "capture empty value for the ContentType header" in {
      val mockRequest = fakeGetRequest.withHeaders((CONTENT_TYPE, ""))

      val result = validateHeadersForNPS(BS_Periods_POST)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, CONTENT_TYPE)
    }

    "capture invalid value for the ContentType header" in {
      val mockRequest = fakeGetRequest.withHeaders((CONTENT_TYPE, "334534534534"))

      val result = validateHeadersForNPS(BS_Periods_POST)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, CONTENT_TYPE)
    }

    "assert a valid ContentType value is valid" in {
      assert(validateHeadersForNPS(BS_Periods_POST)(fakeGetRequest).isValid)
    }
  }

  "RequestValidation.validateStaffPid" should {
    "capture missing StaffPid header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.StaffPid)

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, Header.StaffPid)
    }

    "capture empty value for the StaffPid header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffPid, ""))

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffPid)
    }

    "capture invalid value for the StaffPid header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffPid, "334534534534"))

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffPid)
    }
  }

  "RequestValidation.validateStaffPidForRequestType" should {
    "capture invalid value for the StaffPid header when RequestType value is ATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffPid, unattendedStaffPid))

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffPid)
    }

    "capture invalid value for the StaffPid header when RequestType value is UNATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders(
        (Header.RequestType, Attended.DA2_BS_UNATTENDED.toString),
        (Header.StaffPid, attendedStaffPid)
      )

      val result = validateHeadersForNPS(BS_Details_GET)(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffPid)
    }

    "assert a StaffPid value is valid when RequestType value is ATTENDED" in {
      assert(validateHeadersForNPS(BS_Details_GET)(fakeGetRequest).isValid)
    }

    "assert a StaffPid value is valid when RequestType value is UNATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders(
        (Header.RequestType, Attended.DA2_BS_UNATTENDED.toString),
        (Header.StaffPid, unattendedStaffPid)
      )

      assert(validateHeadersForNPS(BS_Details_GET)(mockRequest).isValid)
    }
  }

  "RequestValidation.retrieveCorrelationId" should {
    "return None for missing CorrelationId header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.CorrelationId)

      assert(retrieveCorrelationId(mockRequest).isEmpty)
    }

    "return Some value for passed CorrelationId header" in {
      assert(retrieveCorrelationId(fakeGetRequest).isDefined)
    }
  }

  private def assertOnlyExpectedErrorPresent[A](
    validated: Validation[A],
    expectedError: BaseError,
    errorMsgContains: String
  ): ErrorItem =
    assertOnlyExpectedErrorPresent(validated, expectedError, Some(errorMsgContains))

  private def assertOnlyExpectedErrorPresent[A](
    validated: Validation[A],
    expectedError: BaseError,
    errorMsgContains: Option[String]
  ): ErrorItem = {
    assert(validated.isInvalid)

    val errors = validated.toEither.left.get
    assert(errors.length == 1)
    errors.head.baseError shouldBe expectedError

    errorMsgContains.map { headerName =>
      assert(errors.head.details.isDefined)
      assert(errors.head.details.get.contains(headerName))
    }

    errors.head
  }
}
