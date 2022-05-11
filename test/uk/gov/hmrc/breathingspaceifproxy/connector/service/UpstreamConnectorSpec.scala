/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.syntax.option._
import org.scalatest.Assertion
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.http.Status
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Periods_POST
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.circuitbreaker.CircuitBreakerConfig
import uk.gov.hmrc.http._

class UpstreamConnectorSpec extends AnyWordSpec with BaseSpec with UpstreamConnector {

  val config = inject[Configuration]

  override protected def circuitBreakerConfig: CircuitBreakerConfig = CircuitBreakerConfig(
    serviceName = config.get[String]("appName"),
    numberOfCallsToTriggerStateChange = Int.MaxValue.some
  )

  "breakOnException" should {
    "return true for HttpException and 5xx response code" in {
      val throwable = new HttpException("message", 500)
      breakOnException(throwable) shouldBe true
    }

    "return false for HttpException and 4xx response code" in {
      val throwable = new HttpException("message", 400)
      breakOnException(throwable) shouldBe false
    }

    "return true for Upstream5xxResponse" in {
      val throwable = Upstream5xxResponse("message", 500, 500)
      breakOnException(throwable) shouldBe true
    }

    "return false for any other Throwable" in {
      val throwable = new Throwable()
      breakOnException(throwable) shouldBe false
    }
  }

  "handleUpstreamError" should {
    "return RESOURCE_NOT_FOUND for a NOT_FOUND response" in {
      verifyResponse(new NotFoundException("Some error message"), BaseError.RESOURCE_NOT_FOUND)
    }

    "return NO_DATA_FOUND for a NOT_FOUND response with specific message" in {
      verifyResponse(new NotFoundException(noDataFound), BaseError.NO_DATA_FOUND)
    }

    "return NOT_IN_BREATHING_SPACE for a NOT_FOUND response with specific message" in {
      verifyResponse(new NotFoundException(notInBS), BaseError.NOT_IN_BREATHING_SPACE)
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
  }

  val upstreamConnector = inject[EisConnector]

  private def verifyResponse(throwable: Throwable, baseError: BaseError): Assertion = {
    val result =
      handleUpstreamError[Unit](genRequestId(BS_Periods_POST, upstreamConnector)).apply(throwable).futureValue

    assert(result.isInvalid)
    assert(result.fold(_.head.baseError == baseError, _ => false))
  }
}
