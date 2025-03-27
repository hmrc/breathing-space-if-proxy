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

package uk.gov.hmrc.breathingspaceifproxy.connector.service

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.breathingspaceifproxy.UpstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.model.RequestId
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class HeaderHandlerSpec extends AnyWordSpec with HeaderHandler with BaseSpec with MockitoSugar {

  val endpointId: EndpointId               = mock[EndpointId]
  val upstreamConnector: UpstreamConnector = mock[UpstreamConnector]

  "headers function" should {
    "return the staffId in the headers if it's different from the unattendedStaffPid" in {

      val requestId: RequestId = genRequestId(endpointId, upstreamConnector)

      val expectedHeaders = Seq(
        "Authorization" -> "Bearer localhost-only-token",
        "Environment"   -> "ist0",
        "CorrelationId" -> requestId.correlationId.toString,
        "OriginatorId"  -> requestId.requestType.entryName,
        "UserId"        -> requestId.staffId
      )

      headers(appConfig, requestId) shouldBe expectedHeaders
    }

    "not return the staffId for unattended request" in {

      val requestId: RequestId = genUnattendedRequestId(endpointId, upstreamConnector)

      val expectedHeaders = Seq(
        "Authorization"              -> "Bearer localhost-only-token",
        "Environment"                -> "ist0",
        UpstreamHeader.CorrelationId -> requestId.correlationId.toString,
        UpstreamHeader.RequestType   -> requestId.requestType.entryName
      )

      headers(appConfig, requestId) shouldBe expectedHeaders
    }
  }
}
