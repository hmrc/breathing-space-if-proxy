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

import scala.concurrent.Future

import cats.syntax.option._
import cats.syntax.validated._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class PeriodsControllerGetSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockConnector: PeriodsConnector = mock[PeriodsConnector]
  val controller = new PeriodsController(
    appConfig,
    inject[AuditConnector],
    Helpers.stubControllerComponents(),
    mockConnector
  )

  "get" should {

    "return 200(OK) when the Nino is valid and all required headers are present" in {
      Given(s"a GET request with a valid Nino and all required headers")
      when(mockConnector.get(any[Nino])(any[RequestId], any[HeaderCarrier]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      val response = controller.get(genNinoString)(fakeGetRequest)
      status(response) shouldBe OK
    }

    s"return 200(OK) when the Nino is valid and all required headers are present, except $CONTENT_TYPE" in {
      Given(s"a GET request with a valid Nino and all required headers, except $CONTENT_TYPE")
      when(mockConnector.get(any[Nino])(any[RequestId], any[HeaderCarrier]))
        .thenReturn(Future.successful(validPeriodsResponse.validNec))

      val response = controller.get(genNinoString)(requestFilteredOutOneHeader(CONTENT_TYPE))
      status(response) shouldBe OK
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      Given(s"a GET request with an invalid Nino")
      val response = controller.get(invalidNino)(fakeGetRequest).run

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_NINO")
      errorList.head.code shouldBe INVALID_NINO.entryName
      assert(errorList.head.message.startsWith(INVALID_NINO.message))
    }

    "return 400(BAD_REQUEST) with multiple errors when the Nino is invalid and one required header is missing" in {
      Given(s"a GET request with an invalid Nino and without the ${Header.StaffPid} request header")
      val response = controller.get("HT1234B")(requestFilteredOutOneHeader(Header.StaffPid)).run

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
