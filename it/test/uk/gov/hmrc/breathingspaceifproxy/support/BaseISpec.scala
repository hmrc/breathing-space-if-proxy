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

package uk.gov.hmrc.breathingspaceifproxy.support

import cats.syntax.option._
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.apache.pekko.stream.Materializer
import org.scalatest._
import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, Status, Writeable}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers.{await, route}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Injecting}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.NOT_AUTHORISED
import uk.gov.hmrc.breathingspaceifproxy.model.enums.{Attended, EndpointId}
import uk.gov.hmrc.breathingspaceifproxy.{DownstreamHeader, UpstreamHeader}

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class BaseISpec
    extends AnyWordSpec
    with BreathingSpaceTestSupport
    with DefaultAwaitTimeout
    with Eventually
    with GivenWhenThen
    with GuiceOneServerPerSuite
    with HeaderNames
    with Injecting
    with Matchers
    with OptionValues
    with WireMockSupport {

  val configProperties: Map[String, Any] = Map(
    "api.access.version-1.0.allowlistedApplicationIds.0"                -> "123456789",
    "api.access.version-1.0.allowlistedApplicationIds.1"                -> "987654321",
    "auditing.enabled"                                                  -> true,
    "auditing.consumer.baseUri.host"                                    -> wireMockHost,
    "auditing.consumer.baseUri.port"                                    -> wireMockPort,
    "circuit.breaker.if.failedCallsInUnstableBeforeUnavailable"         -> Int.MaxValue,
    "circuit.breaker.memorandum.failedCallsInUnstableBeforeUnavailable" -> Int.MaxValue,
    "microservice.services.auth.host"                                   -> wireMockHost,
    "microservice.services.auth.port"                                   -> wireMockPort,
    "microservice.services.integration-framework.host"                  -> wireMockHost,
    "microservice.services.integration-framework.port"                  -> wireMockPort
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(configProperties)
      .build()

  implicit lazy val materializer: Materializer = inject[Materializer]

  implicit lazy val appConfig: AppConfig = inject[AppConfig]

  def fakeAttendedRequest(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withHeaders(requestHeaders: _*)

  def fakeUnattendedRequest(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withHeaders(requestHeadersForUnattended: _*)

  def fakeMemorandumRequest(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withHeaders(requestHeadersForMemorandum: _*)

  def verifyAuditEventCall(endpointId: EndpointId): Assertion = {
    val body         = s"""{"auditSource":"breathing-space-if-proxy", "auditType":"${endpointId.auditType}"}"""
    val requestedFor =
      postRequestedFor(urlEqualTo("/write/audit"))
        .withRequestBody(equalToJson(body, true, true))

    eventually(timeout(5 seconds), interval(200 millis)) {
      verify(1, requestedFor)
    }

    Succeeded
  }

  def verifyHeaders(method: HttpMethod, url: String): Unit =
    verifyHeadersForAttended(method, url)

  def verifyHeaders(method: HttpMethod, url: String, queryParam: Map[String, String]): Unit =
    verifyHeadersForAttended(method, url, queryParam)

  def verifyHeadersForAttended(method: HttpMethod, url: String): Unit =
    verifyHeadersForAttended(
      method.requestedFor(urlPathMatching(url))
    )

  def verifyHeadersForAttended(method: HttpMethod, url: String, queryParam: Map[String, String]): Unit =
    if (queryParam.isEmpty) {
      verifyHeadersForAttended(method, url)
    } else {
      verifyHeadersForAttended(
        method.requestedFor(urlPathMatching(url)).withQueryParam(queryParam.head._1, equalTo(queryParam.head._2))
      )
    }

  def verifyHeadersForUnattended(method: HttpMethod, url: String): Unit =
    verifyHeadersForUnattended(
      method.requestedFor(urlPathMatching(url))
    )

  def verifyHeadersForUnattended(method: HttpMethod, url: String, queryParam: Map[String, String]): Unit =
    if (queryParam.isEmpty) {
      verifyHeadersForUnattended(method, url)
    } else {
      verifyHeadersForUnattended(
        method.requestedFor(urlPathMatching(url)).withQueryParam(queryParam.head._1, equalTo(queryParam.head._2))
      )
    }

  def verifyHeadersForMemorandum(method: HttpMethod, url: String): Unit =
    verifyHeadersForMemorandum(
      method.requestedFor(urlPathMatching(url))
    )

  private def verifyHeadersForAttended(requestPatternBuilder: RequestPatternBuilder): Unit =
    verify(
      1,
      requestPatternBuilder
        .withHeader(UpstreamHeader.Authorization, equalTo(appConfig.integrationFrameworkAuthToken))
        .withHeader(UpstreamHeader.Environment, equalTo(appConfig.integrationFrameworkEnvironment))
        .withHeader(UpstreamHeader.CorrelationId, equalTo(correlationIdAsString))
        .withHeader(UpstreamHeader.RequestType, equalTo(Attended.DA2_BS_ATTENDED.entryName))
    )

  private def verifyHeadersForUnattended(requestPatternBuilder: RequestPatternBuilder): Unit =
    verify(
      1,
      requestPatternBuilder
        .withHeader(UpstreamHeader.Authorization, equalTo(appConfig.integrationFrameworkAuthToken))
        .withHeader(UpstreamHeader.Environment, equalTo(appConfig.integrationFrameworkEnvironment))
        .withHeader(UpstreamHeader.CorrelationId, equalTo(correlationIdAsString))
        .withHeader(UpstreamHeader.RequestType, equalTo(Attended.DA2_BS_UNATTENDED.entryName))
        .withoutHeader(UpstreamHeader.StaffPid)
    )

  private def verifyHeadersForMemorandum(requestPatternBuilder: RequestPatternBuilder): Unit =
    verify(
      1,
      requestPatternBuilder
        .withHeader(UpstreamHeader.Authorization, equalTo(appConfig.integrationFrameworkAuthToken))
        .withHeader(UpstreamHeader.Environment, equalTo(appConfig.integrationFrameworkEnvironment))
        .withHeader(UpstreamHeader.CorrelationId, equalTo(correlationIdAsString))
        .withHeader(UpstreamHeader.RequestType, equalTo(Attended.DA2_PTA.entryName))
        .withoutHeader(UpstreamHeader.StaffPid)
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
      headers(DownstreamHeader.CorrelationId).toLowerCase shouldBe correlationId.toLowerCase
    }

    And("the body should be in Json format")
    headers.get("Cache-Control")            shouldBe Some(appConfig.httpHeaderCacheControl)
    headers(CONTENT_TYPE).toLowerCase       shouldBe MimeTypes.JSON.toLowerCase
    result.body.contentType.get.toLowerCase shouldBe MimeTypes.JSON.toLowerCase
    val bodyAsJson = Json.parse(result.body.consumeData.futureValue.utf8String)

    And(s"""contain an "errors" list with $numberOfErrors detail errors""")
    val errorList = (bodyAsJson \ "errors").as[List[TestingErrorItem]]
    errorList.size shouldBe numberOfErrors
    errorList
  }

  def verifyUnauthorized[T](request: Request[T])(implicit w: Writeable[T]): Assertion = {
    unauthorized()
    val response = await(route(app, request).get)

    val errorList = verifyErrorResult(response, Status.UNAUTHORIZED, correlationIdAsString.some, 1)

    And(s"the error code should be $NOT_AUTHORISED")
    errorList.head.code                          shouldBe NOT_AUTHORISED.entryName
    assert(errorList.head.message.startsWith(NOT_AUTHORISED.message))
    response.header.headers.get("Cache-Control") shouldBe Some(appConfig.httpHeaderCacheControl)
  }
}
