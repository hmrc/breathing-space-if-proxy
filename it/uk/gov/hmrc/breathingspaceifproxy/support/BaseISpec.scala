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

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import akka.stream.Materializer
import akka.util.Timeout
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.contentAsJson
import uk.gov.hmrc.breathingspaceifproxy.{HeaderContext, HeaderCorrelationId}
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.Attended

abstract class BaseISpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneServerPerSuite
    with OptionValues
    with TestData
    with WireMockSupport {

  implicit lazy val defaultAwaitTimeout: Timeout = 5 seconds
  implicit lazy val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  val configProperties: Map[String, Any] = Map(
    "auditing.enabled" -> "false",
    "auditing.traceRequests" -> "false",
    "metrics.enabled" -> "false",
    "microservice.services.integration-framework.host" -> wireMockHost,
    "microservice.services.integration-framework.port" -> wireMockPort,
  )

  override lazy val fakeApplication: Application =
    GuiceApplicationBuilder()
      .configure(configProperties)
      .build()

  implicit lazy val materializer: Materializer = fakeApplication.materializer

  lazy val appConfig: AppConfig = fakeApplication.injector.instanceOf[AppConfig]

  lazy val localContext: String = "breathing-space"

  def fakeRequest(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withHeaders(
      HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
      HeaderCorrelationId -> UUID.randomUUID.toString,
      HeaderContext -> Attended.PEGA_UNATTENDED.toString
    )

  def reason(result: Future[Result]): String = (contentAsJson(result) \ "reason").as[String]
}
