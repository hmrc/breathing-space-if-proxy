/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.implicits._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.connector.service.EisConnector
import uk.gov.hmrc.breathingspaceifproxy.connector.MemorandumConnector
import uk.gov.hmrc.breathingspaceifproxy.model.{MemorandumInResponse, Nino, RequestId}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.{INVALID_NINO, MISSING_HEADER}
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class MemorandumControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockUpstreamConnector = mock[EisConnector]
  when(mockUpstreamConnector.currentState).thenReturn("HEALTHY")

  val mockConnector: MemorandumConnector = mock[MemorandumConnector]
  when(mockConnector.eisConnector).thenReturn(mockUpstreamConnector)

  val controller = new MemorandumController(
    appConfig,
    inject[AuditConnector],
    authConnector,
    Helpers.stubControllerComponents(),
    mockConnector
  )

  "get" should {
    "return 200(OK) when the Nino is valid and all required headers are present" in {
      val memorandum = MemorandumInResponse(true)

      when(mockConnector.get(any[Nino])(any[RequestId]))
        .thenReturn(Future.successful(memorandum.validNec))

      val response = controller.get(genNinoString)(fakeGetRequest)

      status(response) shouldBe OK
      contentAsString(response) shouldBe Json.toJson(memorandum).toString
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      val response = controller.get("AA00A")(fakeGetRequest).run

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      errorList.head.code shouldBe INVALID_NINO.entryName
      assert(errorList.head.message.startsWith(INVALID_NINO.message))
    }

    "return 400(BAD_REQUEST) when correlation id is missing" in {
      val response =
        controller
          .get("AA000001A")(attendedRequestFilteredOutOneHeader(DownstreamHeader.CorrelationId))
          .run

      val errorList = verifyErrorResult(response, BAD_REQUEST, none, 1)

      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.startsWith(MISSING_HEADER.message))
    }

    "return 400(BAD_REQUEST) when request type is missing" in {
      val response =
        controller
          .get("AA000001A")(attendedRequestFilteredOutOneHeader(DownstreamHeader.RequestType))
          .run

      verifyMissingHeader(response)
    }

    "return 400(BAD_REQUEST) when staff pid is missing" in {
      val response =
        controller
          .get("AA000001A")(attendedRequestFilteredOutOneHeader(DownstreamHeader.StaffPid))
          .run

      verifyMissingHeader(response)
    }
  }

  def verifyMissingHeader(response: Future[Result]): Unit = {
    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    errorList.head.code shouldBe MISSING_HEADER.entryName
    assert(errorList.head.message.startsWith(MISSING_HEADER.message))
  }
}
