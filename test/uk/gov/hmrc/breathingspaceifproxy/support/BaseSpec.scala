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
import org.scalatest.{Assertion, GivenWhenThen, Informing, OptionValues, TestSuite}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{DefaultAwaitTimeout, Injecting}
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.Header.CorrelationId
import uk.gov.hmrc.breathingspaceifproxy.model.Error.httpErrorIds
import uk.gov.hmrc.http.HeaderCarrier

trait BaseSpec
    extends DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneAppPerSuite
    with Informing
    with Injecting
    with Matchers
    with OptionValues { this: TestSuite =>

  implicit lazy val materializer: Materializer = inject[Materializer]

  implicit lazy val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(
    Header.CorrelationId -> correlationId
  )

  lazy val correlationId = UUID.randomUUID().toString

  def verifyErrorResult(
    future: Future[Result],
    expectedStatus: Int,
    expectedMessage: String,
    withCorrelationId: Boolean
  ): Assertion = {

    val result = future.futureValue
    Then(s"the resulting Response should have as Http Status $expectedStatus")
    val responseHeader = result.header
    responseHeader.status shouldBe expectedStatus

    And("a body in Json format")
    val headers = responseHeader.headers
    headers.size shouldBe 2
    headers.get(CONTENT_TYPE) shouldBe Some(MimeTypes.JSON)

    if (withCorrelationId) {
      And("a \"Correlation-Id\" header")
      headers.get(CorrelationId) shouldBe Some(CorrelationId)
    }

    And("the expected Body")
    result.body.contentType shouldBe Some(MimeTypes.JSON)
    val bodyAsJson = Json.parse(result.body.consumeData.futureValue.utf8String)

    And("the body should contain an \"errors\" list with 1 detail error")
    val errorList = (bodyAsJson \ "errors").as[List[ErrorT]]
    errorList.size shouldBe 1
    errorList.head.code shouldBe httpErrorIds.get(expectedStatus).head
    errorList.head.message shouldBe expectedMessage
  }
}
