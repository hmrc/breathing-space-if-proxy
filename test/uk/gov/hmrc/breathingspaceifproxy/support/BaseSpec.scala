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

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
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
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig

import scala.annotation.nowarn
import scala.concurrent.Future

object BaseSpec {
  implicit class retrievalsTestingSyntax[A](val a: A) extends AnyVal {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }
}

@nowarn("msg=dead code following this construct")
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

  import BaseSpec._

  implicit lazy val materializer: Materializer = inject[Materializer]

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(1, Seconds), interval = Span(250, Millis))

  override implicit val appConfig: AppConfig = inject[AppConfig]

  val authConnector: AuthConnector = mock[AuthConnector]

  type AuthRetrieval = Option[String] ~ Option[TrustedHelper] ~ Option[String]
  val result: AuthRetrieval = None ~ None ~ Some("client-id")

  when(authConnector.authorise[AuthRetrieval](any(), any())(any(), any()))
    .thenReturn(Future.successful(result))

  def verifyErrorResult(
    future: Future[Result],
    expectedStatus: Int,
    correlationId: Option[String],
    numberOfErrors: Int
  ): List[TestingErrorItem] = {

    val result         = future.futureValue
    Then(s"the resulting response should have as Http Status $expectedStatus")
    val responseHeader = result.header
    responseHeader.status shouldBe expectedStatus

    val headers = responseHeader.headers

    correlationId.fold[Assertion](headers.size shouldBe 1) { correlationId =>
      And("a \"Correlation-Id\" header")
      headers(DownstreamHeader.CorrelationId).toLowerCase shouldBe correlationId.toLowerCase
    }

    And("the body should be in Json format")
    headers(CONTENT_TYPE).toLowerCase       shouldBe MimeTypes.JSON.toLowerCase
    result.body.contentType.get.toLowerCase shouldBe MimeTypes.JSON.toLowerCase
    val bodyAsJson = Json.parse(result.body.consumeData.futureValue.utf8String)

    And(s"""contain an "errors" list with $numberOfErrors detail errors""")
    val errorList = (bodyAsJson \ "errors").as[List[TestingErrorItem]]
    errorList.size shouldBe numberOfErrors
    errorList
  }
}
