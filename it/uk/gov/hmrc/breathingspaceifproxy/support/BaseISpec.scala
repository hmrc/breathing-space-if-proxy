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
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Injecting}
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.Attended

abstract class BaseISpec
  extends AnyWordSpec
    with BreathingSpaceTestSupport
    with DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneServerPerSuite
    with HeaderNames
    with Injecting
    with Matchers
    with OptionValues
    with WireMockSupport {

  val configProperties: Map[String, Any] = Map(
    "api.access.version-1.0.whitelistedApplicationIds.0" -> "123456789",
    "api.access.version-1.0.whitelistedApplicationIds.1" -> "987654321",
    "microservice.services.integration-framework.host" -> wireMockHost,
    "microservice.services.integration-framework.port" -> wireMockPort
  )

  override val fakeApplication: Application =
    GuiceApplicationBuilder()
      .configure(configProperties)
      .build()

  implicit val materializer = inject[Materializer]

  implicit val appConfig: AppConfig = inject[AppConfig]

  def urlWithoutQuery(url: String): String = url.split("\\?").head

  def fakeRequest(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withHeaders(requestHeaders: _*)

  def fakeRequestForUnattended(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withHeaders(requestHeadersForUnattended: _*)

  def verifyHeaders(method: HttpMethod, url: String): Unit =
    verifyHeadersForAttended(method, url)

  def verifyHeaders(method: HttpMethod, url: String, queryParam: Map[String, String]): Unit =
    verifyHeadersForAttended(method, url, queryParam)

  def verifyHeadersForAttended(method: HttpMethod, url: String): Unit =
    verifyHeadersForAttended(
      method.verifyHeaderFor(urlPathMatching(url))
    )

  def verifyHeadersForAttended(method: HttpMethod, url: String, queryParam: Map[String, String]): Unit =
    if (queryParam.isEmpty) verifyHeadersForAttended(method, url)
    else verifyHeadersForAttended(
      method.verifyHeaderFor(urlPathMatching(url)).withQueryParam(queryParam.head._1, equalTo(queryParam.head._2))
    )

  def verifyHeadersForUnattended(method: HttpMethod, url: String): Unit =
    verifyHeadersForUnattended(
      method.verifyHeaderFor(urlPathMatching(url))
    )

  def verifyHeadersForUnattended(method: HttpMethod, url: String, queryParam: Map[String, String]): Unit =
    if (queryParam.isEmpty) verifyHeadersForUnattended(method, url)
    else verifyHeadersForUnattended(
      method.verifyHeaderFor(urlPathMatching(url)).withQueryParam(queryParam.head._1, equalTo(queryParam.head._2))
    )

  private def verifyHeadersForAttended(requestPatternBuilder: RequestPatternBuilder): Unit =
    verify(1, requestPatternBuilder
      .withHeader(Header.Authorization, equalTo(appConfig.integrationframeworkAuthToken))
      .withHeader(Header.Environment, equalTo(appConfig.integrationFrameworkEnvironment))
      .withHeader(retrieveHeaderMapping(Header.CorrelationId), equalTo(correlationIdAsString))
      .withHeader(retrieveHeaderMapping(Header.RequestType), equalTo(Attended.DA2_BS_ATTENDED.entryName))
      .withHeader(retrieveHeaderMapping(Header.StaffPid), equalTo(attendedStaffPid))
    )

  private def verifyHeadersForUnattended(requestPatternBuilder: RequestPatternBuilder): Unit =
    verify(1, requestPatternBuilder
      .withHeader(Header.Authorization, equalTo(appConfig.integrationframeworkAuthToken))
      .withHeader(Header.Environment, equalTo(appConfig.integrationFrameworkEnvironment))
      .withHeader(retrieveHeaderMapping(Header.CorrelationId), equalTo(correlationIdAsString))
      .withHeader(retrieveHeaderMapping(Header.RequestType), equalTo(Attended.DA2_BS_UNATTENDED.entryName))
      .withoutHeader(retrieveHeaderMapping(Header.StaffPid))
    )

  def verifyErrorResult(
    result: Result,
    expectedStatus: Int,
    correlationId: Option[String],
    numberOfErrors: Int
  ): List[TestingErrorItem] = {

    Then(s"the resulting response should have as Http Status $expectedStatus")
    val responseHeader = result.header
    responseHeader.status shouldBe expectedStatus

    val headers = responseHeader.headers

    correlationId.fold[Assertion](headers.size shouldBe 1) { correlationId =>
      And("a \"Correlation-Id\" header")
      headers.get(Header.CorrelationId).get.toLowerCase shouldBe correlationId.toLowerCase
    }

    And("the body should be in Json format")
    headers.get(CONTENT_TYPE).get.toLowerCase shouldBe MimeTypes.JSON.toLowerCase
    result.body.contentType.get.toLowerCase shouldBe MimeTypes.JSON.toLowerCase
    val bodyAsJson = Json.parse(result.body.consumeData.futureValue.utf8String)

    And(s"""contain an "errors" list with $numberOfErrors detail errors""")
    val errorList = (bodyAsJson \ "errors").as[List[TestingErrorItem]]
    errorList.size shouldBe numberOfErrors
    errorList
  }
}
