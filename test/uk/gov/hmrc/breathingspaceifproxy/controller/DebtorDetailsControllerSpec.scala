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

import java.util.UUID

import scala.concurrent.Future

import cats.syntax.option._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.{Result, Results}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.Header._
import uk.gov.hmrc.breathingspaceifproxy.connector.DebtorDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.Nino
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier

class DebtorDetailsControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockConnector: DebtorDetailsConnector = mock[DebtorDetailsConnector]
  val controller = new DebtorDetailsController(appConfig, Helpers.stubControllerComponents(), mockConnector)

  "get" should {

    "return 200(OK) when all required headers are present and a valid NINO" in {
      Given("a request with all required headers and valid NINO")
      when(mockConnector.get(any[Nino])(any[HeaderCarrier])).thenReturn(Future.successful(Results.Status(OK)))

      val response: Future[Result] = controller.get("HT423277B")(fakeRequest)

      status(response) shouldBe OK
    }

    s"return 400(BAD_REQUEST) when the $CorrelationId header is missing" in {
      Given(s"a request without the $CorrelationId request header")
      val invalidRequest = FakeRequest().withHeaders(validHeaders.filter(_._1 != Header.CorrelationId): _*)

      val response: Future[Result] = controller.get("HT423277B")(invalidRequest)

      val errorList = verifyErrorResult(response, BAD_REQUEST, None, 1)

      And("the error code should be MISSING_HEADER")
      errorList.size shouldBe 1
      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.contains(Header.CorrelationId))
    }

    s"return 400(BAD_REQUEST) when the $RequestType header is missing" in {
      Given(s"a request without the $RequestType request header")
      val invalidRequest = FakeRequest().withHeaders(validHeaders.filter(_._1 != Header.RequestType): _*)

      val response: Future[Result] = controller.get("HT423277B")(invalidRequest)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationId.some, 1)

      And("the error code should be MISSING_HEADER")
      errorList.size shouldBe 1
      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.contains(Header.RequestType))
    }

    s"return 400(BAD_REQUEST) when the $StaffId header is missing" in {
      Given(s"a request without the $StaffId request header")
      val invalidRequest = FakeRequest().withHeaders(validHeaders.filter(_._1 != Header.StaffId): _*)

      val response: Future[Result] = controller.get("HT423277B")(invalidRequest)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationId.some, 1)

      And("the error code should be MISSING_HEADER")
      errorList.size shouldBe 1
      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.contains(Header.StaffId))
    }

    "return 400(BAD_REQUEST) when all required headers are missing" in {
      Given("a request without any of the requested headers")
      val response: Future[Result] = controller.get("HT423277B")(FakeRequest())

      val errorList = verifyErrorResult(response, BAD_REQUEST, None, 3)

      And("the error code for all elements should be MISSING_HEADER")
      assert(errorList.forall(_.code == MISSING_HEADER.entryName))
    }

    "return 400(BAD_REQUEST) when the Nino is invalid and all required headers are missing" in {
      Given("a request without any of the requested headers")
      val response: Future[Result] = controller.get("HT12345B")(FakeRequest())

      val errorList = verifyErrorResult(response, BAD_REQUEST, None, 4)

      And("the error code for 3 elements should be MISSING_HEADER")
      errorList.filter(_.code == MISSING_HEADER.entryName).size shouldBe 3

      And("the error code for 1 element should be INVALID_NINO")
      errorList.filter(_.code == INVALID_NINO.entryName).size shouldBe 1
    }

    "return 400(BAD_REQUEST) when the Nino is invalid and one required header is missing" in {
      val correlationId = UUID.randomUUID().toString
      Given("a request without any of the requested headers")
      val response: Future[Result] = controller.get("HT12345B")(
        FakeRequest().withHeaders(Header.CorrelationId -> correlationId, Header.StaffId -> "1234567")
      )

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationId.some, 2)

      And("the error code for 1 element should be MISSING_HEADER")
      errorList.filter(_.code == MISSING_HEADER.entryName).size shouldBe 1

      And("the error code for 1 element should be INVALID_NINO")
      errorList.filter(_.code == INVALID_NINO.entryName).size shouldBe 1
    }
  }
}
