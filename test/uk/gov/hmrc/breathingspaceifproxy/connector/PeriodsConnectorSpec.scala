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

import java.util

import scala.concurrent._

import com.codahale.metrics.{MetricRegistry, Timer}
import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchers.anyString
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Writes}
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http._

class PeriodsConnectorSpec extends AnyWordSpec with BaseSpec with BeforeAndAfterEach with MockitoSugar {

  private val mockHttpClient = mock[HttpClient]
  private val mockMetrics = mock[Metrics]
  private val mockMetricRegistry = mock[MetricRegistry]

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
        s"http://localhost:9601/individuals/breathing-space/NINO/${nino.value}/periods"

      Then(s"then the composed url should equal $expectedUrl")
      PeriodsConnector.url(nino) shouldBe expectedUrl
    }
  }

  type SeqOfHeader = List[(String, String)]

  def returnResponseFromIF[T](eventualResponse: T): Unit =
    when(
      mockHttpClient.POST[JsValue, T](anyString, any[JsValue], any[SeqOfHeader])(
        any[Writes[JsValue]],
        any[HttpReads[T]],
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(Future.successful(eventualResponse))
}
