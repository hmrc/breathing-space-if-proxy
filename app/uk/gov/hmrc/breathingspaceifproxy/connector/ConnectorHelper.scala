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

import scala.concurrent.Future

import cats.syntax.validated._
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.http._

trait ConnectorHelper extends HttpErrorFunctions with Logging {

  def handleUpstreamError[T](implicit requestId: RequestId): PartialFunction[Throwable, ResponseValidation[T]] = {
    case UpstreamErrorResponse.Upstream4xxResponse(response) => handleUpstream4xxError(response)
    case UpstreamErrorResponse.Upstream5xxResponse(response) => handleUpstream5xxError(response)
    case throwable: Throwable =>
      logger.error(s"Exception caught for downstream request $requestId. ${throwable.getMessage}")
      Future.successful(ErrorItem(SERVER_ERROR).invalidNec)
  }

  private def handleUpstream4xxError[T](response: UpstreamErrorResponse)(implicit r: RequestId): ResponseValidation[T] =
    response.statusCode match {
      case Status.NOT_FOUND => logErrorAndGenUpstreamResponse(response, RESOURCE_NOT_FOUND)
      case Status.CONFLICT => logErrorAndGenUpstreamResponse(response, CONFLICTING_REQUEST)
      case _ => logErrorAndGenUpstreamResponse(response, SERVER_ERROR)
    }

  private def handleUpstream5xxError[T](response: UpstreamErrorResponse)(implicit r: RequestId): ResponseValidation[T] =
    response.statusCode match {
      case Status.BAD_GATEWAY => logErrorAndGenUpstreamResponse(response, DOWNSTREAM_BAD_GATEWAY)
      case Status.SERVICE_UNAVAILABLE => logErrorAndGenUpstreamResponse(response, DOWNSTREAM_SERVICE_UNAVAILABLE)
      case Status.GATEWAY_TIMEOUT => logErrorAndGenUpstreamResponse(response, DOWNSTREAM_TIMEOUT)
      case _ => logErrorAndGenUpstreamResponse(response, SERVER_ERROR)
    }

  private def logErrorAndGenUpstreamResponse[T](response: UpstreamErrorResponse, baseError: BaseError)(
    implicit requestId: RequestId
  ): ResponseValidation[T] = {
    logger.error(s"Error(${response.statusCode}) for $requestId. ${response.message}")
    Future.successful(ErrorItem(baseError).invalidNec)
  }
}
