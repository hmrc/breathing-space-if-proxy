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

import cats.syntax.validated._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.Assertion
import play.api.libs.json.Json
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class RequestValidationSpec extends AnyWordSpec with BaseSpec {

  val validator = new RequestValidation() {}

  "RequestValidation.validateNino" should {
    "capture empty Nino value is invalid" in {
      assert(validator.validateNino("").isInvalid)
    }

    "capture invalid Nino value is invalid" in {
      assert(validator.validateNino("werr").isInvalid)
    }

    "assert a valid Nino value is valid" in {
      assert(validator.validateNino(validNinoAsString).isValid)
    }
  }

  "RequestValidation.validateCorrelationId" should {
    "capture missing CorrelationId header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.CorrelationId)

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, Header.CorrelationId)
    }

    "capture empty value for the CorrelationId header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.CorrelationId, ""))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.CorrelationId)
    }

    "capture invalid value for the CorrelationId header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.CorrelationId, "334534534534"))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.CorrelationId)
    }

    "assert a valid CorrelationId value is valid" in {
      assert(validator.validateHeadersForNPS(fakeGetRequest).isValid)
    }
  }

  "RequestValidation.validateRequestType" should {
    "capture missing RequestType header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.RequestType)

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, Header.RequestType)
    }

    "capture empty value for the RequestType header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.RequestType, ""))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.RequestType)
    }

    "capture invalid value for the RequestType header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.RequestType, "334534534534"))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.RequestType)
    }

    "assert a RequestType value of ATTENDED is valid" in {
      assert(validator.validateHeadersForNPS(fakeGetRequest).isValid)
    }

    "assert a RequestType value of UNATTENDED is valid" in {
      val mockRequest = fakeGetRequest.withHeaders(
        (Header.RequestType, Attended.DS2_BS_UNATTENDED.toString),
        (Header.StaffId, unattendedStaffId)
      )

      assert(validator.validateHeadersForNPS(mockRequest).isValid)
    }
  }

  // TODO: Check that this is even possible on API Platform
  "RequestValidation.validateContentType" should {
    "capture missing ContentType header for non-GET methods" in {
      val mockRequest = requestFilteredOutOneHeader(CONTENT_TYPE, "POST")

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, CONTENT_TYPE)
    }

    "allow missing ContentType header for GET methods" in {
      val mockRequest = requestFilteredOutOneHeader(CONTENT_TYPE)

      assert(validator.validateHeadersForNPS(mockRequest).isValid)
    }

    "capture empty value for the ContentType header" in {
      val mockRequest = fakeGetRequest.withHeaders((CONTENT_TYPE, ""))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, CONTENT_TYPE)
    }

    "capture invalid value for the ContentType header" in {
      val mockRequest = fakeGetRequest.withHeaders((CONTENT_TYPE, "334534534534"))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, CONTENT_TYPE)
    }

    "assert a valid ContentType value is valid" in {
      assert(validator.validateHeadersForNPS(fakeGetRequest).isValid)
    }
  }

  "RequestValidation.validateStaffId" should {
    "capture missing StaffId header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.StaffId)

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, Header.StaffId)
    }

    "capture empty value for the StaffId header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffId, ""))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffId)
    }

    "capture invalid value for the StaffId header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffId, "334534534534"))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffId)
    }
  }

  "RequestValidation.validateStaffIdForRequestType" should {
    "capture invalid value for the StaffId header when RequestType value is ATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffId, unattendedStaffId))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffId)
    }

    "capture invalid value for the StaffId header when RequestType value is UNATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders(
        (Header.RequestType, Attended.DS2_BS_UNATTENDED.toString),
        (Header.StaffId, attendedStaffId)
      )

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffId)
    }

    "assert a StaffId value is valid when RequestType value is ATTENDED" in {
      assert(validator.validateHeadersForNPS(fakeGetRequest).isValid)
    }

    "assert a StaffId value is valid when RequestType value is UNATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders(
        (Header.RequestType, Attended.DS2_BS_UNATTENDED.toString),
        (Header.StaffId, unattendedStaffId)
      )

      assert(validator.validateHeadersForNPS(mockRequest).isValid)
    }
  }

  "RequestValidation.validateBody" should {
    val mockValidator = (cpr: CreatePeriodsRequest) => ValidatedCreatePeriodsRequest(nino, List.empty).validNec

    "capture missing request body" in {
      val result = validator.validateBody[CreatePeriodsRequest, ValidatedCreatePeriodsRequest](mockValidator(_))(
        fakeGetRequest,
        CreatePeriodsRequest.format
      )

      assertOnlyExpectedErrorPresent(result, MISSING_BODY)
    }

    "capture request body contains Json that does not match expected schema" in {
      val mockRequest = fakeGetRequest.withJsonBody(Json.parse("{}"))

      val result = validator.validateBody[CreatePeriodsRequest, ValidatedCreatePeriodsRequest](mockValidator(_))(
        mockRequest,
        CreatePeriodsRequest.format
      )

      assertOnlyExpectedErrorPresent(result, INVALID_JSON)
    }
  }

  "RequestValidation.retrieveCorrelationId" should {
    "return None for missing CorrelationId header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.CorrelationId)

      assert(validator.retrieveCorrelationId(mockRequest).isEmpty)
    }

    "return Some value for passed CorrelationId header" in {
      assert(validator.retrieveCorrelationId(fakeGetRequest).isDefined)
    }
  }

  private def assertOnlyExpectedErrorPresent[A](
    validated: Validation[A],
    expectedError: BaseError,
    missingHeaderName: String
  ): Option[Assertion] =
    assertOnlyExpectedErrorPresent(validated, expectedError, Some(missingHeaderName))

  private def assertOnlyExpectedErrorPresent[A](
    validated: Validation[A],
    expectedError: BaseError,
    missingHeaderName: Option[String] = None
  ): Option[Assertion] = {
    /* When back to original way as the error msg the unit test prints out when this fails is
       confusing and very verbose!

      assert( validated.fold(errors => {
      assert(errors.length == 1)
      errors.head.baseError shouldBe expectedError
      assert(errors.head.details.isDefined)
      errors.head.details.get.contains(missingHeaderName)
    }, _ => false))*/

    assert(validated.isInvalid)

    val errors = validated.toEither.left.get
    assert(errors.length == 1)
    errors.head.baseError shouldBe expectedError

    missingHeaderName.map { headerName =>
      assert(errors.head.details.isDefined)
      assert(errors.head.details.get.contains(headerName))
    }
  }
}
