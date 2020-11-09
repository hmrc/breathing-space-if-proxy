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
import org.scalatest.Assertion
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Request, Result}
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.Header.StaffPid
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier

class IndividualDetailsControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
  val controller = new IndividualDetailsController(appConfig, Helpers.stubControllerComponents(), mockConnector)

  "getMinimalPopulation" should {

    "return 200(OK) when the Nino is valid and all required headers are present" in {
      val nino = genNino
      verifyResponse[Detail0](nino, DetailData0, detail0(nino), '0', fakeGetRequest)
    }

    s"return 200(OK) when the Nino is valid and all required headers are present, except $CONTENT_TYPE" in {
      val nino = genNino
      verifyResponse[Detail0](nino, DetailData0, detail0(nino), '0', requestFilteredOutOneHeader(CONTENT_TYPE))
    }

    "return 200(OK) and the expected individual details when detailId is equal to '1'" in {
      val nino = genNino
      verifyResponse[Detail1](nino, DetailData1, detail1(nino), '1', fakeGetRequest)
    }

    "return 200(OK) and the expected individual details when detailId is equal to 's' (full population)" in {
      val nino = genNino
      verifyResponse[IndividualDetails](nino, FullDetails, details(nino), 's', fakeGetRequest)
    }

    "return 400(BAD_REQUEST) when the detailId provided is invalid" in {
      Given(s"a GET request with a valid Nino and an invalid detailId")
      verifyBadRequest(controller.get(genNinoString, 'Z')(fakeGetRequest).run, INVALID_DETAIL_INDEX)
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      Given(s"a GET request with an invalid Nino and a valid detailId")
      verifyBadRequest(controller.get("HT1234B", '0')(fakeGetRequest).run, INVALID_NINO)
    }

    "return 400(BAD_REQUEST) with multiple errors when the Nino is invalid and one required header is missing" in {
      Given(s"a GET request with an invalid Nino and without the $StaffPid request header")
      val response = controller.get("HT1234B", '0')(requestFilteredOutOneHeader(StaffPid)).run

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 2)

      Then(s"the 1st error code should be $MISSING_HEADER")
      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.startsWith(MISSING_HEADER.message))

      And(s"the 2nd error code should be $INVALID_NINO")
      errorList.last.code shouldBe INVALID_NINO.entryName
      assert(errorList.last.message.startsWith(INVALID_NINO.message))
    }
  }

  private def verifyResponse[T <: Detail](
    nino: Nino,
    detailData: DetailsData[T],
    detail: T,
    detailId: Char,
    request: Request[_]
  ): Assertion = {
    implicit val format: OFormat[T] = detailData.format

    when(mockConnector.get[T](any[Nino], any[DetailsData[T]])(any[RequestId], any[HeaderCarrier]))
      .thenReturn(Future.successful(detail.validNec[ErrorItem]))

    val response = controller.get(nino.value, detailId)(request)
    status(response) shouldBe OK
    contentAsString(response) shouldBe Json.toJson(detail).toString
  }

  private def verifyBadRequest(response: Future[Result], baseError: BaseError): Assertion = {
    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    Then(s"the error code should be ${baseError.entryName}")
    errorList.head.code shouldBe baseError.entryName
    assert(errorList.head.message.startsWith(baseError.message))
  }
}
