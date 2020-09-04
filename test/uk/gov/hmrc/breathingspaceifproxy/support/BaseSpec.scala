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
import org.scalatest.{GivenWhenThen, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.breathingspaceifproxy.HeaderCorrelationId
import uk.gov.hmrc.http.HeaderCarrier

trait BaseSpec
    extends AnyWordSpec
    with DefaultAwaitTimeout
    with GivenWhenThen
    with GuiceOneAppPerSuite
    with Injecting
    with Matchers
    with OptionValues {

  implicit lazy val materializer: Materializer = inject[Materializer]

  implicit lazy val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(
    HeaderCorrelationId -> correlationId
  )

  lazy val correlationId = UUID.randomUUID().toString
}
