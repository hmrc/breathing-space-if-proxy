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
import play.api.libs.json._
import play.api.mvc.{AnyContent, AnyContentAsText}
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.RequestPeriod._
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
        (Header.StaffPid, unattendedStaffPid)
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

  "RequestValidation.validateStaffPid" should {
    "capture missing StaffPid header" in {
      val mockRequest = requestFilteredOutOneHeader(Header.StaffPid)

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, MISSING_HEADER, Header.StaffPid)
    }

    "capture empty value for the StaffPid header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffPid, ""))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffPid)
    }

    "capture invalid value for the StaffPid header" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffPid, "334534534534"))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffPid)
    }
  }

  "RequestValidation.validateStaffPidForRequestType" should {
    "capture invalid value for the StaffPid header when RequestType value is ATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders((Header.StaffPid, unattendedStaffPid))

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffPid)
    }

    "capture invalid value for the StaffPid header when RequestType value is UNATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders(
        (Header.RequestType, Attended.DS2_BS_UNATTENDED.toString),
        (Header.StaffPid, attendedStaffPid)
      )

      val result = validator.validateHeadersForNPS(mockRequest)

      assertOnlyExpectedErrorPresent(result, INVALID_HEADER, Header.StaffPid)
    }

    "assert a StaffPid value is valid when RequestType value is ATTENDED" in {
      assert(validator.validateHeadersForNPS(fakeGetRequest).isValid)
    }

    "assert a StaffPid value is valid when RequestType value is UNATTENDED" in {
      val mockRequest = fakeGetRequest.withHeaders(
        (Header.RequestType, Attended.DS2_BS_UNATTENDED.toString),
        (Header.StaffPid, unattendedStaffPid)
      )

      assert(validator.validateHeadersForNPS(mockRequest).isValid)
    }
  }

  "RequestValidation.validateBody" should {
    val mockValidator = (_: CreatePeriodsRequest) => ValidatedCreatePeriodsRequest(nino, List.empty).validNec

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

    "capture request body that is not of content type Json" in {
      val mockRequest = fakeGetRequest.withBody[AnyContent](AnyContentAsText(""))

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

  "RequestValidation.constructErrorsFromJsResultException" should {
    "create a Validation for a JsResultException that contains a single date field error" in {
      val errorPath = s"""/periods(0)/$StartDateKey"""
      val mockJsException = JsResultException(
        Seq(
          (JsPath(List(KeyPathNode(errorPath))), Seq(JsonValidationError(Seq(""))))
        )
      )

      val result = validator.constructErrorsFromJsResultException[Unit](mockJsException)

      assertOnlyExpectedErrorPresent(result, INVALID_DATE, errorPath)
    }

    "create a Validation for a JsResultException that contains multiple date field errors" in {
      val errorPath1 = s"""/periods(0)/$StartDateKey"""
      val errorPath2 = s"""/periods(1)/$EndDateKey"""
      val errorPath3 = s"""/periods(3)/$PegaRequestTimestampKey"""
      val mockJsException = JsResultException(
        Seq(
          (JsPath(List(KeyPathNode(errorPath1))), Seq(JsonValidationError(Seq("")))),
          (JsPath(List(KeyPathNode(errorPath2))), Seq(JsonValidationError(Seq("")))),
          (JsPath(List(KeyPathNode(errorPath3))), Seq(JsonValidationError(Seq(""))))
        )
      )

      val result = validator.constructErrorsFromJsResultException[Unit](mockJsException)

      assertAllErrorsAreAsExpected(result, INVALID_DATE, 3)
    }

    "create a Validation for a JsResultException that contains a different type of error other than a date field error" in {
      val errorPath = """/periods(0)/SomeOtherField"""
      val mockJsException = JsResultException(
        Seq(
          (JsPath(List(KeyPathNode(errorPath))), Seq(JsonValidationError(Seq(""))))
        )
      )

      val result = validator.constructErrorsFromJsResultException[Unit](mockJsException)

      assertOnlyExpectedErrorPresent(result, INVALID_JSON)
    }
  }

  private def assertOnlyExpectedErrorPresent[A](
    validated: Validation[A],
    expectedError: BaseError,
    errorMsgContains: String
  ): Error =
    assertOnlyExpectedErrorPresent(validated, expectedError, Some(errorMsgContains))

  private def assertOnlyExpectedErrorPresent[A](
    validated: Validation[A],
    expectedError: BaseError,
    errorMsgContains: Option[String] = None
  ): Error = {
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

  private def assertAllErrorsAreAsExpected[A](
    validated: Validation[A],
    expectedError: BaseError,
    errorCount: Int
  ) = {
    assert(validated.isInvalid)

    val errors = validated.toEither.left.get
    assert(errors.length == errorCount)
    assert(errors.filter(_.baseError == expectedError).length == errors.length)
  }
}
