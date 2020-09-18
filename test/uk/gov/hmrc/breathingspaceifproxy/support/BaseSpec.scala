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

import java.util.UUID

import scala.concurrent.Future

import akka.stream.Materializer
import cats.syntax.option._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Injecting}
import uk.gov.hmrc.breathingspaceifproxy.{Header, JsonContentType}
import uk.gov.hmrc.breathingspaceifproxy.Header.CorrelationId
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.Attended
import uk.gov.hmrc.http.HeaderCarrier

trait BaseSpec
    extends DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneAppPerSuite
    with Informing
    with Injecting
    with Matchers
    with OptionValues
    with TestData { this: TestSuite =>

  implicit lazy val materializer: Materializer = inject[Materializer]

  implicit lazy val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(
    Header.CorrelationId -> correlationId
  )

  lazy val appConfig: AppConfig = inject[AppConfig]

  lazy val correlationId = UUID.randomUUID().toString

  lazy val validHeaders = List(
    CONTENT_TYPE -> JsonContentType,
    Header.CorrelationId -> correlationId,
    Header.RequestType -> Attended.DS2_BS_ATTENDED.toString,
    Header.StaffId -> "1234567"
  )

  lazy val fakeGetRequest = FakeRequest().withHeaders(validHeaders: _*)

  def correlationIdAsOpt(withCorrelationId: => Boolean): Option[String] =
    if (withCorrelationId) correlationId.some else None

  def requestWithAllHeaders(method: String = "GET"): FakeRequest[AnyContentAsEmpty.type] =
    requestFilteredOutOneHeader("", method)

  def requestFilteredOutOneHeader(
    headerToFilterOut: String,
    method: String = "GET"
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "/").withHeaders(
      validHeaders.filter(_._1.toLowerCase != headerToFilterOut.toLowerCase): _*
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
      headers.get(CorrelationId) shouldBe correlationId.some
      headers.size shouldBe 2
    }

    And("the body should be in Json format")
    headers.get(CONTENT_TYPE) shouldBe Some(MimeTypes.JSON)
    result.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(result.body.consumeData.futureValue.utf8String)

    And(s"""contain an "errors" list with $numberOfErrors detail errors""")
    val errorList = (bodyAsJson \ "errors").as[List[ErrorItem]]
    errorList.size shouldBe numberOfErrors
    errorList
  }
}
