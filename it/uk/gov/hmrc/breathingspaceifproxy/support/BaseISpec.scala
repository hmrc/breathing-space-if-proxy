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

package uk.gov.hmrc.breathingspaceifproxy.support

import akka.stream.Materializer
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.Attended

abstract class BaseISpec
  extends AnyWordSpec
    with BreathingSpaceTestData
    with DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneServerPerSuite
    with HeaderNames
    with Matchers
    with OptionValues
    with WireMockSupport {

  def configProperties: Map[String, Any] = Map(
    "internalServiceHostPatterns" -> List("localhost"),
    "microservice.services.integration-framework.host" -> wireMockHost,
    "microservice.services.integration-framework.port" -> wireMockPort
  )

  override lazy val fakeApplication: Application =
    GuiceApplicationBuilder()
      .configure(configProperties)
      .build()

  lazy val wsClient: WSClient = fakeApplication.injector.instanceOf[WSClient]

  val testServerAddress = s"http://localhost:$port"

  implicit lazy val materializer: Materializer = fakeApplication.materializer

  override implicit lazy val appConfig: AppConfig = fakeApplication.injector.instanceOf[AppConfig]

  def fakeRequest(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withHeaders(requestHeaders: _*)

  def verifyHeadersForGet(url: String): Unit =
    verify(1, getRequestedFor(urlMatching(url))
      .withHeader(retrieveHeaderMapping(Header.CorrelationId), equalTo(correlationIdAsString))
      .withHeader(retrieveHeaderMapping(Header.RequestType), equalTo(Attended.DS2_BS_ATTENDED.entryName))
      .withHeader(retrieveHeaderMapping(Header.StaffId), equalTo(attendedStaffId))
    )

  def verifyHeadersForPost(url: String): Unit =
    verify(1, postRequestedFor(urlMatching(url))
      .withHeader(retrieveHeaderMapping(Header.CorrelationId), equalTo(correlationIdAsString))
      .withHeader(retrieveHeaderMapping(Header.RequestType), equalTo(Attended.DS2_BS_ATTENDED.entryName))
      .withHeader(retrieveHeaderMapping(Header.StaffId), equalTo(attendedStaffId))
    )
}
