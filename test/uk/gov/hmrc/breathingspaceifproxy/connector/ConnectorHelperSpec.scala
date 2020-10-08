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

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.funsuite.AnyFunSuite
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.SERVER_ERROR
import uk.gov.hmrc.breathingspaceifproxy.model.EndpointId.Breathing_Space_Periods_GET

class ConnectorHelperSpec extends AnyFunSuite with ConnectorHelper {

  test("handleUpstreamError should return SERVER_ERROR for any Throwable caught while sending downstream a request") {
    val requestId = RequestId(Breathing_Space_Periods_GET, UUID.randomUUID)
    val throwable = new IllegalArgumentException("Some illegal argument")

    val result = handleUpstreamError[Unit](requestId).apply(throwable).futureValue

    assert(result.isInvalid)
    assert(result.fold(_.head.baseError == SERVER_ERROR, _ => false))
  }
}
