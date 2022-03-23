/*
 * Copyright 2022 HM Revenue & Customs
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

import scala.concurrent.{ExecutionContext, Future}

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc.Result
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.breathingspaceifproxy.{unit, DownstreamHeader}
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier

trait BaseSpec
    extends BreathingSpaceTestSupport
    with DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneAppPerSuite
    with HeaderNames
    with Informing
    with Injecting
    with Matchers
    with OptionValues
    with ScalaFutures { this: TestSuite =>

  implicit lazy val materializer: Materializer = inject[Materializer]

  implicit val defaultPatience = PatienceConfig(timeout = Span(1, Seconds), interval = Span(250, Millis))

  override implicit val appConfig: AppConfig = inject[AppConfig]

  val authConnector = mock[AuthConnector]

  when(authConnector.authorise(any[Predicate], any[Retrieval[Unit]])(any[HeaderCarrier], any[ExecutionContext]))
    .thenReturn(Future.successful(unit))

  def verifyErrorResult(
    future: Future[Result],
    expectedStatus: Int,
    correlationId: Option[String],
    numberOfErrors: Int
  ): List[TestingErrorItem] = {

    val result = future.futureValue
    Then(s"the resulting response should have as Http Status $expectedStatus")
    val responseHeader = result.header
    responseHeader.status shouldBe expectedStatus

    val headers = responseHeader.headers

    correlationId.fold[Assertion](headers.size shouldBe 1) { correlationId =>
      And("a \"Correlation-Id\" header")
      headers.get(DownstreamHeader.CorrelationId).get.toLowerCase shouldBe correlationId.toLowerCase
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
