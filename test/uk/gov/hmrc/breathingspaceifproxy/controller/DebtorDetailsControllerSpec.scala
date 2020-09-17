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

import org.mockito.scalatest.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.{Result, Results}
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.DebtorDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.model.Nino
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier

class DebtorDetailsControllerSpec extends AnyWordSpec with BaseSpec with MockitoSugar {

  val mockConnector: DebtorDetailsConnector = mock[DebtorDetailsConnector]
  val controller = new DebtorDetailsController(appConfig, Helpers.stubControllerComponents(), mockConnector)

  "get" should {

    "return 200(OK) when all required headers are present and the Nino is valid" in {
      Given(s"a request with all required headers and valid NINO")
      when(mockConnector.get(any[Nino])(any[HeaderCarrier])).thenReturn(Future.successful(Results.Status(OK)))

      val response: Future[Result] = controller.get("HT423277B")(fakeRequest)

      status(response) shouldBe OK
    }

    "return 200(OK) when all required headers for a GET are present and the Nino is valid" in {
      Given(s"a request with all required headers for a GET ($CONTENT_TYPE is not required) and valid NINO")
      when(mockConnector.get(any[Nino])(any[HeaderCarrier])).thenReturn(Future.successful(Results.Status(OK)))

      val response: Future[Result] = controller.get("HT423277B")(requestWithoutOneHeader(CONTENT_TYPE))

      status(response) shouldBe OK
    }
  }
}
