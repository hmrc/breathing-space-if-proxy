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

import scala.concurrent.Future

import akka.stream.Materializer
import cats.syntax.option._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Injecting}
import uk.gov.hmrc.breathingspaceifproxy.{Header, RequestPeriods}
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.CreatePeriodsRequest

trait BaseSpec
    extends BreathingSpaceTestData
    with DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneAppPerSuite
    with HeaderNames
    with Informing
    with Injecting
    with Matchers
    with OptionValues { this: TestSuite =>

  implicit lazy val materializer: Materializer = inject[Materializer]

  override implicit val appConfig: AppConfig = inject[AppConfig]

  lazy val fakeGetRequest = FakeRequest().withHeaders(requestHeaders: _*)

  def correlationIdAsOpt(withCorrelationId: => Boolean): Option[String] =
    if (withCorrelationId) correlationIdAsString.some else None

  def createPeriodsRequest(periods: RequestPeriods): JsValue =
    Json.toJson(CreatePeriodsRequest(validNinoAsString, periods))

  val requestWithAllHeaders: FakeRequest[AnyContentAsEmpty.type] =
    requestFilteredOutOneHeader("", "GET")

  def requestWithAllHeaders(method: String = "GET"): FakeRequest[AnyContentAsEmpty.type] =
    requestFilteredOutOneHeader("", method)

  def requestFilteredOutOneHeader(
    headerToFilterOut: String,
    method: String = "GET"
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "/").withHeaders(
      requestHeaders.filter(_._1.toLowerCase != headerToFilterOut.toLowerCase): _*
    )

  def verifyErrorResult(
    future: Future[Result],
    expectedStatus: Int,
    correlationId: Option[String],
    numberOfErrors: Int
  ): List[ErrorItem] = {

    val result = future.futureValue
    Then(s"the resulting response should have as Http Status $expectedStatus")
    val responseHeader = result.header
    responseHeader.status shouldBe expectedStatus

    val headers = responseHeader.headers

    correlationId.fold[Assertion](headers.size shouldBe 1) { correlationId =>
      And("a \"Correlation-Id\" header")
      headers.get(Header.CorrelationId) shouldBe correlationId.some
      headers.size shouldBe 2
    }

    And("the body should be in Json format")
    headers.get(CONTENT_TYPE).get.toLowerCase shouldBe MimeTypes.JSON.toLowerCase
    result.body.contentType.get.toLowerCase shouldBe MimeTypes.JSON.toLowerCase
    val bodyAsJson = Json.parse(result.body.consumeData.futureValue.utf8String)

    And(s"""contain an "errors" list with $numberOfErrors detail errors""")
    val errorList = (bodyAsJson \ "errors").as[List[ErrorItem]]
    errorList.size shouldBe numberOfErrors
    errorList
  }
}
