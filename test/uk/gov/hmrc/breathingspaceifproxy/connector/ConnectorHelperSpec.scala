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
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.EndpointId.BS_Periods_GET
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.{BadGatewayException, ServiceUnavailableException}

class ConnectorHelperSpec extends AnyWordSpec with BaseSpec with ConnectorHelper {

  "handleUpstreamError" should {
    "return DOWNSTREAM_BAD_GATEWAY for a BadGatewayException while sending downstream a request" in {
      val requestId = RequestId(BS_Periods_GET, UUID.randomUUID)
      val exception = new BadGatewayException("The downstream service is not responding")

      val result = handleUpstreamError[Unit](requestId).apply(exception).futureValue

      assert(result.isInvalid)
      assert(result.fold(_.head.baseError == DOWNSTREAM_BAD_GATEWAY, _ => false))
    }

    "return DOWNSTREAM_SERVICE_UNAVAILABLE for a ServiceUnavailableException while sending downstream a request" in {
      val requestId = RequestId(BS_Periods_GET, UUID.randomUUID)
      val exception = new ServiceUnavailableException("The downstream service is unavailable")

      val result = handleUpstreamError[Unit](requestId).apply(exception).futureValue

      assert(result.isInvalid)
      assert(result.fold(_.head.baseError == DOWNSTREAM_SERVICE_UNAVAILABLE, _ => false))
    }

    "return SERVER_ERROR for any Throwable caught while sending downstream a request" in {
      val requestId = RequestId(BS_Periods_GET, UUID.randomUUID)
      val throwable = new IllegalArgumentException("Some illegal argument")

      val result = handleUpstreamError[Unit](requestId).apply(throwable).futureValue

      assert(result.isInvalid)
      assert(result.fold(_.head.baseError == SERVER_ERROR, _ => false))
    }
  }
}
