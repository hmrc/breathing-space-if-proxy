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

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import java.util.UUID

import scala.concurrent.Future

import cats.syntax.option._
import cats.syntax.validated._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.Assertion
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.connector.service.EisConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class PeriodsControllerPutSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockDownstreamConnector = mock[EisConnector]
  when(mockDownstreamConnector.currentState).thenReturn("HEALTHY")

  val mockConnector: PeriodsConnector = mock[PeriodsConnector]
  when(mockConnector.eisConnector).thenReturn(mockDownstreamConnector)

  val controller = new PeriodsController(
    appConfig,
    inject[AuditConnector],
    authConnector,
    Helpers.stubControllerComponents(),
    mockConnector
  )

  "put" should {

    "return 200(OK) when all required headers are present and the body is valid Json" in {
      when(mockConnector.put(any[Nino], any[PutPeriodsInRequest])(any[RequestId]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      Given("a request with all required headers and a valid Json body")
      val request = unattendedRequestWithAllHeaders(PUT).withBody(putPeriodsRequest(putPeriodsRequest))

      val response = controller.put(genNinoString)(request)
      status(response) shouldBe OK
    }

    "return 200(OK) when for a period the endDate is not present" in {
      when(mockConnector.put(any[Nino], any[PutPeriodsInRequest])(any[RequestId]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      Given("a Period where the endDate is missing")
      val body = putPeriodsRequest(List(PutPeriodInRequest(UUID.randomUUID, LocalDate.now, None, ZonedDateTime.now)))

      And("a request with all required headers and the Period as a valid Json body")
      val request = unattendedRequestWithAllHeaders(PUT).withBody(body)

      val response = controller.put(genNinoString)(request)
      status(response) shouldBe OK
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      val body = putPeriodsRequest(putPeriodsRequest)
      val request = unattendedRequestWithAllHeaders(PUT).withBody(body)

      val response = controller.put(invalidNino)(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_NINO")
      errorList.head.code shouldBe INVALID_NINO.entryName
      assert(errorList.head.message.startsWith(INVALID_NINO.message))
    }

    "return 400(BAD_REQUEST) when the 'periods' array is empty" in {
      val body = Json.obj("periods" -> List.empty[PutPeriodInRequest]).validNec[ErrorItem]
      val request = unattendedRequestWithAllHeaders(PUT).withBody(body)

      val response = controller.put(genNinoString)(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_PERIODS")
      errorList.head.code shouldBe MISSING_PERIODS.entryName
      assert(errorList.head.message.startsWith(MISSING_PERIODS.message))
    }

    "return 400(BAD_REQUEST) when 'periods' is not an array" in {
      val body = Json.obj("periods" -> validPutPeriod).validNec[ErrorItem]
      val request = unattendedRequestWithAllHeaders(PUT).withBody(body)

      val response = controller.put(genNinoString)(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_PERIODS")
      errorList.head.code shouldBe MISSING_PERIODS.entryName
      assert(errorList.head.message.startsWith(MISSING_PERIODS.message))
    }

    "return 400(BAD_REQUEST) when format of periodID is invalid" in {
      verifyJsonItemValidation(INVALID_JSON_ITEM, "1234567890".some, "2020-04-30")
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
    periodId: Option[String],
    startDate: String,
    endDate: Option[String] = None,
    timestamp: String = ZonedDateTime.now.toString
  ): Assertion = {
    val body = putPeriodsRequestAsJson(periodId.fold(periodIdAsString)(identity), startDate, endDate, timestamp)
    val request = unattendedRequestWithAllHeaders(PUT).withBody(body)

    val response = controller.put(genNinoString)(request)

    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    And(s"the error code should be $error")
    errorList.head.code shouldBe error.entryName
    assert(errorList.head.message.startsWith(error.message))
  }

  private def putPeriodsRequestAsJson(
    periodId: String,
    startDate: String,
    endDate: Option[String],
    timestamp: String
  ): Validation[JsValue] = {
    val pi = s""""$periodIdKey":"$periodId""""
    val sd = s""""$startDateKey":"$startDate""""
    val ed = endDate.fold("")(v => s""","$endDateKey":"$v"""")
    val ts = s""""$timestampKey":"$timestamp""""

    Json.parse(s"""{"periods":[{$pi,$sd$ed,$ts}]}""").validNec[ErrorItem]
  }
}
