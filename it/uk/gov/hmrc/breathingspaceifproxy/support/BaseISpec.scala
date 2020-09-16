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

import akka.stream.Materializer
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.breathingspaceifproxy.Header._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.Attended

abstract class BaseISpec
  extends AnyWordSpec
    with DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneServerPerSuite
    with Matchers
    with OptionValues
    with TestData
    with WireMockSupport {

  def configProperties: Map[String, Any] = Map(
    "microservice.services.integration-framework.host" -> wireMockHost,
    "microservice.services.integration-framework.port" -> wireMockPort
  )

  override lazy val fakeApplication: Application =
    GuiceApplicationBuilder()
      .configure(configProperties)
      .build()

  implicit lazy val materializer: Materializer = fakeApplication.materializer

  implicit lazy val appConfig: AppConfig = fakeApplication.injector.instanceOf[AppConfig]

  def fakeRequest(method: String, path: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withHeaders(
      HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
      CorrelationId -> UUID.randomUUID.toString,
      RequestType -> Attended.DS2_BS_UNATTENDED.toString,
      StaffId -> "1234567"
    )
}