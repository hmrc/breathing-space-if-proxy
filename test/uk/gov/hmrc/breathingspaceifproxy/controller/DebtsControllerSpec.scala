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

import cats.syntax.option._
import cats.syntax.validated._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.connector.DebtsConnector
import uk.gov.hmrc.breathingspaceifproxy.connector.service.EtmpConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.util.UUID
import scala.concurrent.Future

class DebtsControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockUpstreamConnector: EtmpConnector = mock[EtmpConnector]
  when(mockUpstreamConnector.currentState).thenReturn("HEALTHY")

  val mockConnector: DebtsConnector = mock[DebtsConnector]
  when(mockConnector.etmpConnector).thenReturn(mockUpstreamConnector)

  val controller = new DebtsController(
    appConfig,
    inject[AuditConnector],
    authConnector,
    Helpers.stubControllerComponents(),
    mockConnector
  )

  "get" should {

    "return 200(OK) when the Nino is valid and all required headers are present" in {
      Given(s"a GET request with a valid Nino and all required headers")
      when(mockConnector.get(any[Nino], any[UUID])(any[RequestId]))
        .thenReturn(Future.successful(Debts(listOfDebts).validNec))

      val response = controller.get(genNinoString, periodIdAsString)(fakeGetRequest)
      status(response) shouldBe OK
    }

    s"return 200(OK) when the Nino is valid and all required headers are present, except $CONTENT_TYPE" in {
      Given(s"a GET request with a valid Nino and all required headers, except $CONTENT_TYPE")
      when(mockConnector.get(any[Nino], any[UUID])(any[RequestId]))
        .thenReturn(Future.successful(Debts(listOfDebts).validNec))

      val response = controller.get(genNinoString, periodIdAsString)(attendedRequestFilteredOutOneHeader(CONTENT_TYPE))
      status(response) shouldBe OK
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      Given(s"a GET request with an invalid Nino")
      val response = controller.get(invalidNino, periodIdAsString)(fakeGetRequest).run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_NINO")
      errorList.head.code shouldBe INVALID_NINO.entryName
      assert(errorList.head.message.startsWith(INVALID_NINO.message))
    }

    "return 400(BAD_REQUEST) when the periodId is invalid" in {
      Given(s"a GET request with an invalid periodId")
      val response = controller.get(genNinoString, "An invalid periodId")(fakeGetRequest).run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_PERIOD_ID")
      errorList.head.code shouldBe INVALID_PERIOD_ID.entryName
      assert(errorList.head.message.startsWith(INVALID_PERIOD_ID.message))
    }

    "return 400(BAD_REQUEST) with multiple errors when the URI params are invalid and one required header is missing" in {
      Given(
        s"a GET request with an invalid Nino, an invalid periodId and without the ${DownstreamHeader.StaffPid} request header"
      )
      val response =
        controller
          .get("HT1234B", "An invalid periodId")(attendedRequestFilteredOutOneHeader(DownstreamHeader.StaffPid))
          .run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 3)

      And(s"the 1st error code should be $MISSING_HEADER")
      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.startsWith(MISSING_HEADER.message))

      And(s"the 2nd error code should be $INVALID_NINO")
      errorList(1).code shouldBe INVALID_NINO.entryName
      assert(errorList(1).message.startsWith(INVALID_NINO.message))

      And(s"the 3rd error code should be $INVALID_PERIOD_ID")
      errorList(2).code shouldBe INVALID_PERIOD_ID.entryName
      assert(errorList(2).message.startsWith(INVALID_PERIOD_ID.message))
    }

    "return 503(SERVICE_UNAVAILABLE) if an error is returned from the Connector" in {
      Given(s"a GET request with a valid Nino and all required headers")
      when(mockConnector.get(any[Nino], any[UUID])(any[RequestId]))
        .thenReturn(Future.successful(ErrorItem(SERVER_ERROR).invalidNec))

      val response = controller.get(genNinoString, periodIdAsString)(fakeGetRequest)
      status(response) shouldBe SERVICE_UNAVAILABLE
    }
  }
}
