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
import scala.concurrent.ExecutionContext.Implicits.global

import cats.syntax.option._
import com.codahale.metrics.{MetricRegistry, Timer}
import com.kenshoo.play.metrics.Metrics
import java.util
import org.mockito.ArgumentMatchers.anyString
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.HeaderNames
import play.api.http.Status.CREATED
import play.api.libs.json.Writes
import play.api.test.Helpers._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.breathingspaceifproxy.{Header, Periods}
import uk.gov.hmrc.breathingspaceifproxy.model.ValidatedCreatePeriodsRequest
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseSpec, TestData}
import uk.gov.hmrc.http._

class PeriodsConnectorSpec extends AnyWordSpec with BaseSpec with BeforeAndAfterEach with MockitoSugar with TestData {

  implicit val appConfiguration = appConfig

  private val mockHttpClient = mock[HttpClient]
  private val mockMetrics = mock[Metrics]
  private val mockMetricRegistry = mock[MetricRegistry]

  private val connector = new PeriodsConnector(mockHttpClient, mockMetrics)

  override protected def beforeEach() = {
    reset(mockHttpClient, mockMetrics, mockMetricRegistry)
    when(mockMetrics.defaultRegistry).thenReturn(mockMetricRegistry)
    when(mockMetricRegistry.getTimers).thenReturn(new util.TreeMap())
    when(mockMetricRegistry.timer(anyString)).thenReturn(new Timer())
  }

  "PeriodsConnector.url" should {
    "correctly compose a url to the IF" in {
      Given("a valid Nino ")
      val expectedUrl =
        s"http://localhost:9601/breathing-space/api/v1/${nino.value}/periods"

      Then(s"then the composed url should equal $expectedUrl")
      PeriodsConnector.url(nino) shouldBe expectedUrl
    }
  }

  "PeriodsConnector.get" should {
    "correctly parse a valid create" ignore {}
  }

  "PeriodsConnector.post" should {
    val sampleResponseBody = """{"dont-care":"what IF returns"}"""

    "handle a valid response from the IF" in {
      val expectedResponse = HttpResponse(CREATED, sampleResponseBody)
      returnResponseForRequest(Future.successful(expectedResponse))

      Given("a ValidatedCreatePeriodsRequest is received")
      val vcpr = ValidatedCreatePeriodsRequest(nino, periods)

      Then(s"then the result status returned should be 201")
      val result = connector.post(vcpr)(validRequiredHeaderSet)
      status(result) shouldBe CREATED

      And("then the result Content-Type should be JSON")
      contentType(result) shouldBe MimeTypes.JSON.some

      And("the contents of the body should be exactly what IF returns")
      contentAsString(result) shouldBe sampleResponseBody

      And("the headers of the response should contain the 'Correlation-Id' key and correct value")
      headers(result).get(Header.CorrelationId) shouldBe validRequiredHeaderSet.correlationId.value.some

      And("the headers of the response should contain the 'Content-Type' key and correct value")
      headers(result).get(HeaderNames.CONTENT_TYPE) shouldBe MimeTypes.JSON.some
    }

    "handle a NOT_FOUND response from the IF" in {
      val expectedResponse = HttpResponse(NOT_FOUND, sampleResponseBody)
      returnResponseForRequest(Future.successful(expectedResponse))

      Given("a ValidatedCreatePeriodsRequest is received")
      val vcpr = ValidatedCreatePeriodsRequest(nino, periods)

      Then(s"then the result status returned should be 500")
      val result = connector.post(vcpr)(validRequiredHeaderSet)
      status(result) shouldBe NOT_FOUND

      And("then the result Content-Type should be JSON")
      contentType(result) shouldBe MimeTypes.JSON.some

      And("the contents of the body should be exactly what IF returns")
      contentAsString(result) shouldBe
        """{"errors":[{"code":"RESOURCE_NOT_FOUND","message":"Resource not found"}]}"""

      And("the headers of the response should contain the 'Correlation-Id' key and correct value")
      headers(result).get(Header.CorrelationId) shouldBe validRequiredHeaderSet.correlationId.value.some

      And("the headers of the response should contain the 'Content-Type' key and correct value")
      headers(result).get(HeaderNames.CONTENT_TYPE) shouldBe MimeTypes.JSON.some
    }

    "handle an invalid response from the IF" in {
      val expectedResponse = HttpResponse(BAD_REQUEST, sampleResponseBody)
      returnResponseForRequest(Future.successful(expectedResponse))

      Given("a ValidatedCreatePeriodsRequest is received")
      val vcpr = ValidatedCreatePeriodsRequest(nino, periods)

      Then(s"then the result status returned should be 500")
      val result = connector.post(vcpr)(validRequiredHeaderSet)
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("then the result Content-Type should be JSON")
      contentType(result) shouldBe MimeTypes.JSON.some

      And("the contents of the body should be exactly what IF returns")
      contentAsString(result) shouldBe
        """{"errors":{"code":"INTERNAL_SERVER_ERROR","message":"Internal server errorAn error occurred in the downstream systems"}}"""

      And("the headers of the response should contain the 'Correlation-Id' key and correct value")
      headers(result).get(Header.CorrelationId) shouldBe validRequiredHeaderSet.correlationId.value.some

      And("the headers of the response should contain the 'Content-Type' key and correct value")
      headers(result).get(HeaderNames.CONTENT_TYPE) shouldBe MimeTypes.JSON.some
    }

    "handle a HttpException being thrown when calling the IF" in {
      returnResponseForRequest(Future.failed(new HttpException("Whoops!", GATEWAY_TIMEOUT)))

      Given("a ValidatedCreatePeriodsRequest is received")
      val vcpr = ValidatedCreatePeriodsRequest(nino, periods)

      Then(s"then the result status returned should be 500")
      val result = connector.post(vcpr)(validRequiredHeaderSet)
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("then the result Content-Type should be JSON")
      contentType(result) shouldBe MimeTypes.JSON.some

      And("the contents of the body should be exactly what IF returns")
      contentAsString(result) shouldBe
        """{"errors":[{"code":"GATEWAY_TIMEOUT","message":"Whoops!"}]}"""

      And("the headers of the response should contain the 'Correlation-Id' key and correct value")
      headers(result).get(Header.CorrelationId) shouldBe validRequiredHeaderSet.correlationId.value.some

      And("the headers of the response should contain the 'Content-Type' key and correct value")
      headers(result).get(HeaderNames.CONTENT_TYPE) shouldBe MimeTypes.JSON.some
    }

    "handle a Throwable being thrown when calling the IF" in {
      returnResponseForRequest(Future.failed(new IllegalArgumentException("Whoops!")))

      Given("a ValidatedCreatePeriodsRequest is received")
      val vcpr = ValidatedCreatePeriodsRequest(nino, periods)

      Then(s"then the result status returned should be 500")
      val result = connector.post(vcpr)(validRequiredHeaderSet)
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("then the result Content-Type should be JSON")
      contentType(result) shouldBe MimeTypes.JSON.some

      And("the contents of the body should be exactly what IF returns")
      contentAsString(result) shouldBe
        """{"errors":[{"code":"INTERNAL_SERVER_ERROR","message":"Whoops!"}]}"""

      And("the headers of the response should contain the 'Correlation-Id' key and correct value")
      headers(result).get(Header.CorrelationId) shouldBe validRequiredHeaderSet.correlationId.value.some

      And("the headers of the response should contain the 'Content-Type' key and correct value")
      headers(result).get(HeaderNames.CONTENT_TYPE) shouldBe MimeTypes.JSON.some
    }
  }

  type SeqOfHeader = Seq[(String, String)]

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) =
    when(
      mockHttpClient.POST[Periods, HttpResponse](anyString, any[Periods], any[SeqOfHeader])(
        any[Writes[Periods]],
        any[HttpReads[HttpResponse]],
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(eventualResponse)
}
