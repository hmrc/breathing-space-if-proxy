/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.Assertion
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.http.Status
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Periods_POST
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http._

class UpstreamConnectorSpec extends AnyWordSpec with BaseSpec with UpstreamConnector {

  val config = inject[Configuration]

  "handleUpstreamError" should {
    "return INTERNAL_SERVER_ERROR for a NOT_FOUND response" in {
      verifyResponse(new NotFoundException("Some error message"), BaseError.INTERNAL_SERVER_ERROR)
    }

    "return NO_DATA_FOUND for a NOT_FOUND response with specific message" in {
      verifyResponse(new NotFoundException(noDataFound), BaseError.NO_DATA_FOUND)
    }

    "return NOT_IN_BREATHING_SPACE for a NOT_FOUND response with specific message" in {
      verifyResponse(new NotFoundException(notInBS), BaseError.NOT_IN_BREATHING_SPACE)
    }

    "return PERIOD_ID_NOT_FOUND for a NOT_FOUND response with specific message" in {
      verifyResponse(new NotFoundException(noPeriodIdFound), BaseError.PERIOD_ID_NOT_FOUND)
    }

    "return RESOURCE_NOT_FOUND for a NOT_FOUND response with specific message (noResourceFound)" in {
      verifyResponse(new NotFoundException(noResourceFound), BaseError.RESOURCE_NOT_FOUND)
    }

    "return RESOURCE_NOT_FOUND for a NOT_FOUND response with specific message (notIdentifierFound)" in {
      verifyResponse(new NotFoundException(notIdentifierFound), BaseError.RESOURCE_NOT_FOUND)
    }

    "return BREATHING_SPACE_EXPIRED for a FORBIDDEN response" in {
      verifyResponse(new ForbiddenException("Some error message"), BaseError.BREATHING_SPACE_EXPIRED)
    }

    "return CONFLICTING_REQUEST for a CONFLICT response" in {
      verifyResponse(new ConflictException("Some error message"), BaseError.CONFLICTING_REQUEST)
    }

    "return UPSTREAM_SERVICE_UNAVAILABLE for a SERVICE_UNAVAILABLE(HttpException) response" in {
      verifyResponse(
        new ServiceUnavailableException("The upstream service is unavailable"),
        BaseError.SERVER_ERROR
      )
    }

    "return UPSTREAM_TIMEOUT for a GATEWAY_TIMEOUT(HttpException) response" in {
      verifyResponse(new GatewayTimeoutException("Request timed out"), BaseError.SERVER_ERROR)
    }

    "return UPSTREAM_BAD_GATEWAY for a BAD_GATEWAY(Upstream5xxResponse) response" in {
      verifyResponse(
        UpstreamErrorResponse("The upstream service is not responding", Status.BAD_GATEWAY),
        BaseError.SERVER_ERROR
      )
    }

    "return UPSTREAM_SERVICE_UNAVAILABLE for a SERVICE_UNAVAILABLE(Upstream5xxResponse) response" in {
      verifyResponse(
        UpstreamErrorResponse("The upstream service is unavailable", Status.SERVICE_UNAVAILABLE),
        BaseError.SERVER_ERROR
      )
    }

    "return UPSTREAM_TIMEOUT for a GATEWAY_TIMEOUT(Upstream5xxResponse) response" in {
      verifyResponse(UpstreamErrorResponse("Request timed out", Status.GATEWAY_TIMEOUT), BaseError.SERVER_ERROR)
    }

    "return FORBIDDEN for a FORBIDDEN response" in {
      verifyResponse(UpstreamErrorResponse("A message", Status.FORBIDDEN), BaseError.BREATHING_SPACE_EXPIRED)
    }

    "return CONFLICT for a CONFLICT response" in {
      verifyResponse(UpstreamErrorResponse("A message", Status.CONFLICT), BaseError.CONFLICTING_REQUEST)
    }

    "return TOO_MANY_REQUESTS for a TOO_MANY_REQUESTS response" in {
      verifyResponse(UpstreamErrorResponse("A message", Status.TOO_MANY_REQUESTS), BaseError.TOO_MANY_REQUESTS)
    }

    "return SERVER_ERROR for any Throwable caught while sending upstream a request" in {
      verifyResponse(new IllegalArgumentException("Some illegal argument"), BaseError.INTERNAL_SERVER_ERROR)
    }

    "return SERVER_ERROR a 400 status except (NOT_FOUND, FORBIDDEN, CONFLICT and TOO_MANY_REQUESTS)" in {
      verifyResponse(UpstreamErrorResponse("A message", Status.IM_A_TEAPOT), BaseError.INTERNAL_SERVER_ERROR)
    }
  }

  val upstreamConnector = inject[EisConnector]

  private def verifyResponse(throwable: Throwable, baseError: BaseError): Assertion = {
    val result =
      handleUpstreamError[Unit](genRequestId(BS_Periods_POST, upstreamConnector)).apply(throwable).futureValue

    assert(result.isInvalid)
    assert(result.fold(_.head.baseError == baseError, _ => false))
  }
}
