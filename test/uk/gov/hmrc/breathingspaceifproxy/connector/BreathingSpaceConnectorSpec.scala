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

package uk.gov.hmrc.breathingspaceifproxy.connector

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpException
import org.mockito.scalatest.MockitoSugar
import org.mockito.ArgumentMatchers.anyString
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.http.Status
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.{BsHeaders, Nino}
import uk.gov.hmrc.breathingspaceifproxy.model.Attended.PEGA_UNATTENDED
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class BreathingSpaceConnectorSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with MockitoSugar {

  private implicit val ec = Helpers.stubControllerComponents().executionContext

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig = new AppConfig(configuration, serviceConfig)

  private val mockHttpClient = mock[HttpClient]
  private val connector = new BreathingSpaceConnector(mockHttpClient, appConfig)

  val validNino = Nino("MZ006526C")
  val validAttended = PEGA_UNATTENDED

  val url: String = s"${appConfig.integrationFrameworkUrl}/debtor/${validNino.value}"

  val bsHeaders = BsHeaders("correlationId", validAttended)
  implicit val hc: HeaderCarrier = BsHeaders.constructHeaderCarrier(bsHeaders)

  type SeqOfHeader = Seq[(String, String)]

  "BreathingSpaceConnector" should {
    "handle being returned a 200" in {
      when(mockHttpClient.GET(anyString)(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(Status.OK, "")))

      val result = await(connector.requestIdentityDetails(validNino, bsHeaders))
      result.status shouldBe Status.OK
    }

    "handle being returned a non 2XX" in {
      when(mockHttpClient.GET(anyString)(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(Status.NOT_FOUND, "")))

      val result = await(connector.requestIdentityDetails(validNino, bsHeaders))
      result.status shouldBe Status.NOT_FOUND
    }

    "handle a HttpException being thrown by HttpClient" in {
      when(mockHttpClient.GET(anyString)(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(new HttpException("Whoopse!", Status.NOT_IMPLEMENTED)))

      val caught = intercept[HttpException] {
        await(connector.requestIdentityDetails(validNino, bsHeaders))
      }
      caught shouldBe a[HttpException]
    }

    "handle a Exception being thrown by HttpClient" in {
      when(mockHttpClient.GET(anyString)(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(new Exception("Big Whoopse!")))

      val caught = intercept[Exception] {
        await(connector.requestIdentityDetails(validNino, bsHeaders))
      }
      caught shouldBe a[Exception]
    }
  }

  override protected def beforeEach(): Unit =
    reset(mockHttpClient)
}
