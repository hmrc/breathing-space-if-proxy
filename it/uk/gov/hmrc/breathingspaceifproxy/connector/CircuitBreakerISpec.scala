/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.UUID

import akka.stream.Materializer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, removeStub, stubFor, urlPathMatching}
import com.github.tomakehurst.wiremock.stubbing.{Scenario, StubMapping}
import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import play.api.test.Helpers.await
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.breathingspaceifproxy.ResponseValidation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.UpstreamConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId._
import uk.gov.hmrc.breathingspaceifproxy.support.{BreathingSpaceTestSupport, HttpMethod, WireMockSupport}
import uk.gov.hmrc.http.HeaderCarrier

abstract class CircuitBreakerISpec
  extends AnyFunSuite
  with BreathingSpaceTestSupport
  with DefaultAwaitTimeout
  with GuiceOneServerPerSuite
  with Injecting
  with Matchers
  with WireMockSupport {

  val failedCalls = 4
  val unavailablePeriodDuration = 500

  val configProperties: Map[String, Any] = Map(
    "circuit.breaker.failedCallsInUnstableBeforeUnavailable" -> failedCalls,
    "circuit.breaker.unavailablePeriodDurationInMillis" -> unavailablePeriodDuration,
    "circuit.breaker.unstablePeriodDurationInMillis" -> 500,
    "microservice.services.integration-framework.host" -> wireMockHost,
    "microservice.services.integration-framework.port" -> wireMockPort
  )

  override val fakeApplication: Application =
    GuiceApplicationBuilder()
      .configure(configProperties)
      .build()

  implicit val materializer = inject[Materializer]

  implicit val appConfig: AppConfig = inject[AppConfig]

  implicit val hc = HeaderCarrier()

  protected def stubsForCircuitBreaker[T](
    httpMethod: HttpMethod,
    url: String,
    status: Integer,
    body: String,
    queryParams: Map[String, String] = Map.empty
  ): StubMapping = {
    val call: MappingBuilder = httpMethod.call(urlPathMatching(url)).withQueryParams(mapQueryParams(queryParams))
    removeStub(call)

    val scenarioId = "verify-circuit-breaker"
    val badGateway = aResponse().withStatus(BAD_GATEWAY)

    (1 to failedCalls).foreach { ix =>
      stubFor {
        call
          .inScenario(scenarioId)
          .whenScenarioStateIs(if (ix == 1) Scenario.STARTED else s"${scenarioId}-${ix - 1}")
          .willReturn(badGateway)
          .willSetStateTo(s"${scenarioId}-${ix}")
      }
    }

    stubFor {
      val response = aResponse()
        .withStatus(status)
        .withBody(body)
        .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)

      call
        .inScenario(scenarioId)
        .whenScenarioStateIs(s"${scenarioId}-${failedCalls}")
        .willReturn(response)
    }
  }

  protected def verifyCircuitBreaker[T](
    call: => ResponseValidation[T],
    expected: T,
    upstreamConnector: UpstreamConnector
  ): Assertion = {

    upstreamConnector.currentState shouldBe "HEALTHY"

    (1 to failedCalls).foreach { ix =>
      val shouldBeBadGatewayOnLeft = await(call).toEither
      shouldBeBadGatewayOnLeft match {
        case Left(error) => error.head.baseError shouldBe UPSTREAM_BAD_GATEWAY
        case _ => assert(false)
      }
      upstreamConnector.currentState shouldBe (if (ix == failedCalls) "UNAVAILABLE" else "UNSTABLE")
    }

    val shouldBeBadGatewayOnLeft = await(call).toEither
    shouldBeBadGatewayOnLeft match {
      case Left(error) => error.head.baseError shouldBe SERVER_ERROR
      case _ => assert(false)
    }
    upstreamConnector.currentState shouldBe "UNAVAILABLE"

    val shouldBeServerErrorOnLeft = await(call).toEither
    shouldBeServerErrorOnLeft match {
      case Left(error) => error.head.baseError shouldBe SERVER_ERROR
      case _ => assert(false)
    }
    upstreamConnector.currentState shouldBe "UNAVAILABLE"

    Thread.sleep(unavailablePeriodDuration)

    (1 to failedCalls).find { ix: Int =>
      val shouldBeExpectedOnRight = await(call).toEither
      shouldBeExpectedOnRight match {
        case Right(actual) => actual shouldBe expected
        case _ => assert(false)
      }
      !(upstreamConnector.currentState == (if (ix == failedCalls) "HEALTHY" else "TRIAL"))
    } shouldBe None
  }
}

class CircuitBreakerForDebtsConnector_GET_ISpec extends CircuitBreakerISpec {
  test("Circuit Breaker should work as expected against failed DebtsConnector.get requests") {
    val nino = genNino
    val url = DebtsConnector.path(nino, periodId)
    val connector = inject[DebtsConnector]
    implicit val requestId = genRequestId(BS_Debts_GET, connector.etmpConnector)

    stubsForCircuitBreaker(HttpMethod.Get, url, OK, debtsAsSentFromEis)
    verifyCircuitBreaker(connector.get(nino, periodId), Debts(listOfDebts), connector.etmpConnector)
  }
}

class CircuitBreakerForIndividualDetailsConnector_GET_Details_ISpec extends CircuitBreakerISpec {
  test("Circuit Breaker should work as expected against failed IndividualDetailsConnector.getDetails requests") {
    val nino = genNino
    val url = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string
    val queryParams = detailQueryParams(IndividualDetails.fields)
    val bsDetails = details(nino)
    val expected = Json.toJson(bsDetails).toString
    val connector = inject[IndividualDetailsConnector]
    implicit val requestId = genRequestId(BS_Details_GET, connector.eisConnector)

    stubsForCircuitBreaker(HttpMethod.Get, url, OK, expected, queryParams)
    verifyCircuitBreaker(connector.getDetails(nino), bsDetails, connector.eisConnector)
  }
}

class CircuitBreakerForPeriodsConnector_GET_ISpec extends CircuitBreakerISpec {
  test("Circuit Breaker should work as expected against failed PeriodsConnector.get requests") {
    val nino = genNino
    val url = PeriodsConnector.path(nino)
    val response = Json.toJson(validPeriodsResponse).toString
    val connector = inject[PeriodsConnector]
    implicit val requestId = genRequestId(BS_Periods_GET, connector.eisConnector)

    stubsForCircuitBreaker(HttpMethod.Get, url, OK, response)
    verifyCircuitBreaker(connector.get(nino), validPeriodsResponse, connector.eisConnector)
  }
}

class CircuitBreakerForPeriodsConnector_POST_ISpec extends CircuitBreakerISpec {
  test("Circuit Breaker should work as expected against failed PeriodsConnector.post requests") {
    val nino = genNino
    val url = PeriodsConnector.path(nino)
    val bodyRequest = postPeriodsRequest()
    val bodyResponse = PeriodsInResponse(
      List(PeriodInResponse(UUID.randomUUID(), validPostPeriod.startDate, validPostPeriod.endDate))
    )
    val connector = inject[PeriodsConnector]
    implicit val requestId = genRequestId(BS_Periods_POST, connector.eisConnector)

    stubsForCircuitBreaker(HttpMethod.Post, url, OK, Json.toJson(bodyResponse).toString)
    verifyCircuitBreaker(connector.post(nino, bodyRequest), bodyResponse, connector.eisConnector)
  }
}

class CircuitBreakerForPeriodsConnector_PUT_ISpec extends CircuitBreakerISpec {
  test("Circuit Breaker should work as expected against failed PeriodsConnector.put requests") {
    val nino = genNino
    val url = PeriodsConnector.path(nino)
    val bodyRequest = List(validPutPeriod)
    val bodyResponse = PeriodsInResponse(
      List(PeriodInResponse(validPutPeriod.periodID, validPutPeriod.startDate, validPutPeriod.endDate))
    )
    val connector = inject[PeriodsConnector]
    implicit val requestId = genRequestId(BS_Periods_PUT, connector.eisConnector)

    stubsForCircuitBreaker(HttpMethod.Put, url, OK, Json.toJson(bodyResponse).toString)
    verifyCircuitBreaker(connector.put(nino, bodyRequest), bodyResponse, connector.eisConnector)
  }
}
