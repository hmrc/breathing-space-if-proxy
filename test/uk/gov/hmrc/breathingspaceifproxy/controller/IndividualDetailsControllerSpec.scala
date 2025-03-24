/*
 * Copyright 2023 HM Revenue & Customs
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
import org.mockito.scalatest.MockitoSugar
import org.scalatest.Assertion
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader.StaffPid
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.connector.service.EisConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class IndividualDetailsControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockUpstreamConnector: EisConnector = mock[EisConnector]
  when(mockUpstreamConnector.currentState).thenReturn("HEALTHY")

  val mockConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
  when(mockConnector.eisConnector).thenReturn(mockUpstreamConnector)

  val controller = new IndividualDetailsController(
    appConfig,
    inject[AuditConnector],
    authConnector,
    Helpers.stubControllerComponents(),
    mockConnector
  )

  "getMinimalPopulation" should {

    "return 200(OK) when the Nino is valid and all required headers are present" in {
      verifyResponse(fakeGetRequest)
    }

    s"return 200(OK) when the Nino is valid and all required headers are present, except $CONTENT_TYPE" in {
      verifyResponse(attendedRequestFilteredOutOneHeader(CONTENT_TYPE))
    }

    "return 400(BAD_REQUEST) when the Nino is invalid" in {
      Given(s"a GET request with an invalid Nino and a valid detailId")
      verifyBadRequest(controller.getDetails("HT1234B")(fakeGetRequest).run(), INVALID_NINO)
    }

    "return 400(BAD_REQUEST) with multiple errors when the Nino is invalid and one required header is missing" in {
      Given(s"a GET request with an invalid Nino and without the $StaffPid request header")
      val response = controller.getDetails("HT1234B")(attendedRequestFilteredOutOneHeader(StaffPid)).run()

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 2)

      Then(s"the 1st error code should be $MISSING_HEADER")
      errorList.head.code shouldBe MISSING_HEADER.entryName
      assert(errorList.head.message.startsWith(MISSING_HEADER.message))

      And(s"the 2nd error code should be $INVALID_NINO")
      errorList.last.code shouldBe INVALID_NINO.entryName
      assert(errorList.last.message.startsWith(INVALID_NINO.message))
    }

    "return 503(SERVICE_UNAVAILABLE) if an error is returned from the Connector" in {
      val nino = genNino
      when(mockConnector.getDetails(any[Nino])(any[RequestId]))
        .thenReturn(Future.successful(ErrorItem(SERVER_ERROR).invalidNec))

      val response = controller.getDetails(nino.value)(fakeGetRequest)
      status(response) shouldBe SERVICE_UNAVAILABLE
    }
  }

  private def verifyResponse(request: Request[_]): Assertion = {
    val nino      = genNino
    val bsDetails = details(nino)
    when(mockConnector.getDetails(any[Nino])(any[RequestId]))
      .thenReturn(Future.successful(bsDetails.validNec[ErrorItem]))

    val response = controller.getDetails(nino.value)(request)
    status(response)          shouldBe OK
    contentAsString(response) shouldBe Json.toJson(bsDetails).toString
  }

  private def verifyBadRequest(response: Future[Result], baseError: BaseError): Assertion = {
    val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

    Then(s"the error code should be ${baseError.entryName}")
    errorList.head.code shouldBe baseError.entryName
    assert(errorList.head.message.startsWith(baseError.message))
  }
}
