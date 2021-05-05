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

class PeriodsControllerPostSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

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

  "post" should {

    "return 201(CREATED) when all required headers are present and the body is valid Json" in {
      when(mockConnector.post(any[Nino], any[PostPeriodsInRequest])(any[RequestId]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      Given("a request with all required headers and a valid Json body")
      val request = unattendedRequestWithAllHeaders(POST).withBody(postPeriodsRequestAsJson(postPeriodsRequest()))

      val response = controller.post(request)
      status(response) shouldBe CREATED
    }

    "return 201(CREATED) when the utr is not provided" in {
      when(mockConnector.post(any[Nino], any[PostPeriodsInRequest])(any[RequestId]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      Given("a request with all required headers and a valid Json body where the utr is not provided")
      val request = unattendedRequestWithAllHeaders(POST).withBody(postPeriodsRequestAsJson(postPeriodsRequest(none)))

      val response = controller.post(request)
      status(response) shouldBe CREATED
    }

    "return 201(CREATED) when for a period the endDate is not present" in {
      when(mockConnector.post(any[Nino], any[PostPeriodsInRequest])(any[RequestId]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      Given("a Period where the endDate is missing")
      val body = postPeriodsRequestAsJson(
        PostPeriodsInRequest(
          randomUUID,
          "9876543210".some,
          List(PostPeriodInRequest(LocalDate.now, None, ZonedDateTime.now))
        )
      )

      And("a request with all required headers and the Period as a valid Json body")
      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)
      status(response) shouldBe CREATED
    }

    "return 400(MISSING_NINO) when the Nino is missing" in {
      val body = Json.toJson(postPeriodsRequest()).validNec[ErrorItem]
      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_NINO")
      errorList.head.code shouldBe MISSING_NINO.entryName
      assert(errorList.head.message.startsWith(MISSING_NINO.message))
    }

    "return 400(MISSING_CONSUMER_REQUEST_ID) when consumerRequestId is missing" in {
      val body = requestPayloadWithError("", """"utr": "9876543210",""")
      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_CONSUMER_REQUEST_ID")
      errorList.head.code shouldBe MISSING_CONSUMER_REQUEST_ID.entryName
      assert(errorList.head.message.startsWith(MISSING_CONSUMER_REQUEST_ID.message))
    }

    "return 400(MISSING_CONSUMER_REQUEST_ID) when consumerRequestId is not in the expected format" in {
      val body = requestPayloadWithError(s""""consumerRequestId": "123456",""")
      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_CONSUMER_REQUEST_ID")
      errorList.head.code shouldBe INVALID_CONSUMER_REQUEST_ID.entryName
      assert(errorList.head.message.startsWith(INVALID_CONSUMER_REQUEST_ID.message))
    }

    "return 400(INVALID_JSON) when the Utr is not of the expected type" in {
      val body = requestPayloadWithError(s""""consumerRequestId": "$randomUUID",""", """"utr": 12345,""")
      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_JSON")
      errorList.head.code shouldBe INVALID_JSON.entryName
      assert(errorList.head.message.startsWith(INVALID_JSON.message))
    }

    "return 400(INVALID_UTR) when the Utr is not in the expected format" in {
      val body = postPeriodsRequestAsJson(postPeriodsRequest("1234567".some))
      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_UTR")
      errorList.head.code shouldBe INVALID_UTR.entryName
      assert(errorList.head.message.startsWith(INVALID_UTR.message))
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      verifyJsonItemValidation(INVALID_NINO, invalidNino.some, "2020-04-01")
    }

    "return 400(BAD_REQUEST) when the 'periods' array is empty" in {
      val body = Json
        .obj(
          "nino" -> genNinoString,
          "consumerRequestId" -> randomUUID,
          "periods" -> List.empty[PostPeriodInRequest]
        )
        .validNec[ErrorItem]

      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_PERIODS")
      errorList.head.code shouldBe MISSING_PERIODS.entryName
      assert(errorList.head.message.startsWith(MISSING_PERIODS.message))
    }

    "return 400(BAD_REQUEST) when the 'periods' array is not provided" in {
      val body = Json.obj("nino" -> genNinoString, "consumerRequestId" -> randomUUID).validNec[ErrorItem]
      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

      val response = controller.post(request)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_PERIODS")
      errorList.head.code shouldBe MISSING_PERIODS.entryName
      assert(errorList.head.message.startsWith(MISSING_PERIODS.message))
    }

    "return 400(BAD_REQUEST) when 'periods' is not an array" in {
      val body = Json
        .obj("nino" -> genNinoString, "consumerRequestId" -> randomUUID, "periods" -> validPostPeriod)
        .validNec[ErrorItem]
      val request = unattendedRequestWithAllHeaders(POST).withBody(body)

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

  def requestPayloadWithError(consumerRequestId: String = "", utr: String = ""): Validation[JsValue] =
    Json.parse(s"""{
       |  "nino": "MZ123456C",
       |  $consumerRequestId
       |  $utr
       |  "periods": [
       |    {
       |      "startDate": "2020-01-01",
       |      "pegaRequestTimestamp": "2020-11-13T20:20:39.000Z"
       |    }
       |  ]
       |}""".stripMargin).validNec[ErrorItem]

  private def verifyJsonItemValidation(
    error: BaseError,
    nino: Option[String],
    startDate: String,
    endDate: Option[String] = None,
    timestamp: String = ZonedDateTime.now.toString
  ): Assertion = {
    val body = postPeriodsRequestAsJson(nino.fold(genNinoString)(identity), randomUUID, startDate, endDate, timestamp)
    val request = unattendedRequestWithAllHeaders(POST).withBody(body)

    val response = controller.post(request)

    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    And(s"the error code should be $error")
    errorList.head.code shouldBe error.entryName
    assert(errorList.head.message.startsWith(error.message))
  }

  private def postPeriodsRequestAsJson(
    nino: String,
    consumerRequestId: UUID,
    startDate: String,
    endDate: Option[String],
    timestamp: String
  ): Validation[JsValue] = {
    val sd = s""""$startDateKey":"$startDate""""
    val ed = endDate.fold("")(v => s""","$endDateKey":"$v"""")
    val ts = s""""$timestampKey":"$timestamp""""

    Json
      .parse(s"""{"nino":"$nino","consumerRequestId":"$consumerRequestId", "periods":[{$sd$ed,$ts}]}""")
      .validNec[ErrorItem]
  }
}
