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

import cats.implicits._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.MemorandumConnector
import uk.gov.hmrc.breathingspaceifproxy.connector.service.MemConnector
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.{MISSING_HEADER, SERVER_ERROR}
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorItem, MemorandumInResponse, Nino, RequestId}
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class MemorandumControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockAppConfig: AppConfig = mock[AppConfig]
  when(mockAppConfig.memorandumFeatureEnabled).thenReturn(true)

  val mockUpstreamConnector: MemConnector = mock[MemConnector]
  when(mockUpstreamConnector.currentState).thenReturn("HEALTHY")

  val mockConnector: MemorandumConnector = mock[MemorandumConnector]
  when(mockConnector.memorandumConnector).thenReturn(mockUpstreamConnector)

  val controller = new MemorandumController(
    mockAppConfig,
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

      val response = controller.get(genNino)(fakeMemorandumGetRequest)

      status(response)          shouldBe OK
      contentAsString(response) shouldBe Json.toJson(memorandum).toString
    }

    "return 400(BAD_REQUEST) when correlation id is missing" in {
      val nino     = Nino("AA000001A")
      val response =
        controller
          .get(nino)(memorandumRequestFilteredOutOneHeader(DownstreamHeader.CorrelationId))
          .run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, none, 1)

      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.startsWith(MISSING_HEADER.message))
    }

    "return 503(SERVICE_UNAVAILABLE) if an error is returned from the Connector" in {
      when(mockConnector.get(any[Nino])(any[RequestId]))
        .thenReturn(Future.successful(ErrorItem(SERVER_ERROR).invalidNec))

      val response = controller.get(genNino)(fakeMemorandumGetRequest)

      status(response) shouldBe SERVICE_UNAVAILABLE
    }

    "Memorandum feature switch should return 501 when flag set to false" in {
      when(mockAppConfig.memorandumFeatureEnabled).thenReturn(false)

      val response = controller.get(genNino)(fakeMemorandumGetRequest)
      status(response) shouldBe NOT_IMPLEMENTED
    }
  }

  def verifyMissingHeader(response: Future[Result]): Unit = {
    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    errorList.head.code shouldBe MISSING_HEADER.entryName
    assert(errorList.head.message.startsWith(MISSING_HEADER.message))
    ()
  }
}
