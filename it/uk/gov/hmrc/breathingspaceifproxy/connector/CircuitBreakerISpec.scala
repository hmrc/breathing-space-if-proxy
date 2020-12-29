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
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Debts_GET
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

  val configProperties: Map[String, Any] = Map(
    "circuit.breaker.failedCallsInUnstableBeforeUnavailable" -> 4,
    "circuit.breaker.unavailablePeriodDurationInMillis" -> 500,
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
  implicit val requestId = genRequestId(BS_Debts_GET) // EndpointId is not relevant for these tests

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
    val failedCalls = appConfig.circuitBreaker.numberOfCallsToTriggerStateChange

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

  protected def verifyCircuitBreaker[T](call: => ResponseValidation[T], expected: T, connector: ConnectorHelper): Assertion = {

    connector.currentCircuitBreakerState shouldBe "HEALTHY"

    val failedCalls = appConfig.circuitBreaker.numberOfCallsToTriggerStateChange
    (1 to failedCalls).foreach { ix =>
      val shouldBeBadGatewayOnLeft = await(call).toEither
      shouldBeBadGatewayOnLeft match {
        case Left(error) => error.head.baseError shouldBe UPSTREAM_BAD_GATEWAY
        case _ => assert(false)
      }
      connector.currentCircuitBreakerState shouldBe (if (ix == failedCalls) "UNAVAILABLE" else "UNSTABLE")
    }

    val shouldBeBadGatewayOnLeft = await(call).toEither
    shouldBeBadGatewayOnLeft match {
      case Left(error) => error.head.baseError shouldBe SERVER_ERROR
      case _ => assert(false)
    }
    connector.currentCircuitBreakerState shouldBe "UNAVAILABLE"

    val shouldBeServerErrorOnLeft = await(call).toEither
    shouldBeServerErrorOnLeft match {
      case Left(error) => error.head.baseError shouldBe SERVER_ERROR
      case _ => assert(false)
    }
    connector.currentCircuitBreakerState shouldBe "UNAVAILABLE"

    Thread.sleep(appConfig.circuitBreaker.unavailablePeriodDuration)

    (1 to failedCalls).find { ix: Int =>
      val shouldBeExpectedOnRight = await(call).toEither
      shouldBeExpectedOnRight match {
        case Right(actual) => actual shouldBe expected
        case _ => assert(false)
      }
      !(connector.currentCircuitBreakerState == (if (ix == failedCalls) "HEALTHY" else "TRIAL"))
    } shouldBe None
  }
}

class CircuitBreakerForDebtsConnector_GET_ISpec extends CircuitBreakerISpec {
  test("Circuit Breaker should work as expected against failed DebtsConnector.get requests") {
    val nino = genNino
    val url = DebtsConnector.path(nino)
    val response = Json.toJson(debts).toString
    val connector = inject[DebtsConnector]

    stubsForCircuitBreaker(HttpMethod.Get, url, OK, response)
    verifyCircuitBreaker(connector.get(nino), debts, connector)
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

    stubsForCircuitBreaker(HttpMethod.Get, url, OK, expected, queryParams)
    verifyCircuitBreaker(connector.getDetails(nino), bsDetails, connector)
  }
}

class CircuitBreakerForPeriodsConnector_GET_ISpec extends CircuitBreakerISpec {
  test("Circuit Breaker should work as expected against failed PeriodsConnector.get requests") {
    val nino = genNino
    val url = PeriodsConnector.path(nino)
    val response = Json.toJson(validPeriodsResponse).toString
    val connector = inject[PeriodsConnector]

    stubsForCircuitBreaker(HttpMethod.Get, url, OK, response)
    verifyCircuitBreaker(connector.get(nino), validPeriodsResponse, connector)
  }
}

class CircuitBreakerForPeriodsConnector_POST_ISpec extends CircuitBreakerISpec {
  test("Circuit Breaker should work as expected against failed PeriodsConnector.post requests") {
    val nino = genNino
    val url = PeriodsConnector.path(nino)
    val bodyRequest = List(validPostPeriod)
    val bodyResponse = PeriodsInResponse(
      List(PeriodInResponse(UUID.randomUUID(), validPostPeriod.startDate, validPostPeriod.endDate))
    )
    val connector = inject[PeriodsConnector]

    stubsForCircuitBreaker(HttpMethod.Post, url, OK, Json.toJson(bodyResponse).toString)
    verifyCircuitBreaker(connector.post(nino, bodyRequest), bodyResponse, connector)
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

    stubsForCircuitBreaker(HttpMethod.Put, url, OK, Json.toJson(bodyResponse).toString)
    verifyCircuitBreaker(connector.put(nino, bodyRequest), bodyResponse, connector)
  }
}
