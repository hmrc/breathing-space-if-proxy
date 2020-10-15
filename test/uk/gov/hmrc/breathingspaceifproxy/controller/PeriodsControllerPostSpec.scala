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

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import scala.concurrent.Future

import cats.syntax.option._
import cats.syntax.validated._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.Assertion
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier

class PeriodsControllerPostSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockConnector: PeriodsConnector = mock[PeriodsConnector]
  val controller = new PeriodsController(appConfig, Helpers.stubControllerComponents(), mockConnector)

  "post" should {

    "return 201(CREATED) when all required headers are present and the body is valid Json" in {
      when(mockConnector.post(any[Nino], any[PostPeriodsInRequest])(any[RequestId], any[HeaderCarrier]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      Given("a request with all required headers and a valid Json body")
      val request = requestWithAllHeaders(POST).withBody(postPeriodsRequest(postPeriodsRequest))

      val response = controller.post(request)
      status(response) shouldBe CREATED
    }

    "return 201(CREATED) when for a period the endDate is not present" in {
      when(mockConnector.post(any[Nino], any[PostPeriodsInRequest])(any[RequestId], any[HeaderCarrier]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      Given("a Period where the endDate is missing")
      val body = postPeriodsRequest(List(PostPeriodInRequest(LocalDate.now, None, ZonedDateTime.now)))

      And("a request with all required headers and the Period as a valid Json body")
      val request = requestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)
      status(response) shouldBe CREATED
    }

    "return 400(BAD_REQUEST) when the Nino is missing" in {
      val body = Json.obj("periods" -> postPeriodsRequest).validNec[ErrorItem]
      val request = requestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_NINO")
      errorList.head.code shouldBe MISSING_NINO.entryName
      assert(errorList.head.message.startsWith(MISSING_NINO.message))
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      verifyJsonItemValidation(INVALID_NINO, invalidNino.some, "2020-04-01")
    }

    "return 400(BAD_REQUEST) when the 'periods' array is empty" in {
      val body = Json
        .obj(
          "nino" -> genNinoString,
          "periods" -> List.empty[PostPeriodInRequest]
        )
        .validNec[ErrorItem]

      val request = requestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_PERIODS")
      errorList.head.code shouldBe MISSING_PERIODS.entryName
      assert(errorList.head.message.startsWith(MISSING_PERIODS.message))
    }

    "return 400(BAD_REQUEST) when the 'periods' array is not provided" in {
      val body = Json.obj("nino" -> genNinoString).validNec[ErrorItem]
      val request = requestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_PERIODS")
      errorList.head.code shouldBe MISSING_PERIODS.entryName
      assert(errorList.head.message.startsWith(MISSING_PERIODS.message))
    }

    "return 400(BAD_REQUEST) when 'periods' is not an array" in {
      val body = Json.obj("nino" -> genNinoString, "periods" -> validPostPeriod).validNec[ErrorItem]
      val request = requestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_PERIODS")
      errorList.head.code shouldBe MISSING_PERIODS.entryName
      assert(errorList.head.message.startsWith(MISSING_PERIODS.message))
    }

    "return 400(BAD_REQUEST) when startDate is not a valid date" in {
      verifyJsonItemValidation(INVALID_JSON_ITEM, None, "2020-04-31")
    }

    "return 400(BAD_REQUEST) when endDate is present but it is not a valid date" in {
      verifyJsonItemValidation(INVALID_JSON_ITEM, None, "2020-01-01", "2020-02-30".some)
    }

    "return 400(BAD_REQUEST) when pegaRequestTimestamp is not in the expected ISO-8601 format" in {
      verifyJsonItemValidation(INVALID_JSON_ITEM, None, "2020-04-01", None, LocalDateTime.now.toString)
    }
  }

  private def verifyJsonItemValidation(
    error: BaseError,
    nino: Option[String],
    startDate: String,
    endDate: Option[String] = None,
    timestamp: String = ZonedDateTime.now.toString
  ): Assertion = {
    val body = postPeriodsRequest(nino.fold(genNinoString)(identity), startDate, endDate, timestamp)
    val request = requestWithAllHeaders(POST).withBody(body)

    val response = controller.post(request)

    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    And(s"the error code should be $error")
    errorList.head.code shouldBe error.entryName
    assert(errorList.head.message.startsWith(error.message))
  }
}
