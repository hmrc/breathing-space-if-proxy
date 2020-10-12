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
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.http._

trait ConnectorHelper extends HttpErrorFunctions with Logging {

  def handleUpstreamError[T](implicit requestId: RequestId): PartialFunction[Throwable, ResponseValidation[T]] = {
    case UpstreamErrorResponse.Upstream4xxResponse(response) if response.statusCode == 404 =>
      logErrorAndGenUpstreamResponse(response, RESOURCE_NOT_FOUND)

    case UpstreamErrorResponse.Upstream4xxResponse(response) if response.statusCode == 409 =>
      logErrorAndGenUpstreamResponse(response, CONFLICTING_REQUEST)

    case UpstreamErrorResponse.Upstream4xxResponse(response) =>
      logErrorAndGenUpstreamResponse(response, SERVER_ERROR)

    case UpstreamErrorResponse.Upstream5xxResponse(response) =>
      logErrorAndGenUpstreamResponse(response, SERVER_ERROR)

    case throwable: Throwable =>
      logger.error(s"Exception caught for downstream request $requestId. ${throwable.getMessage}")
      Future.successful(ErrorItem(SERVER_ERROR).invalidNec)
  }

  private def logErrorAndGenUpstreamResponse[T](response: UpstreamErrorResponse, baseError: BaseError)(
    implicit requestId: RequestId
  ): ResponseValidation[T] = {

    logger.error(s"Error(${response.statusCode}) for $requestId. ${response.message}")
    Future.successful(ErrorItem(baseError).invalidNec)
  }
}
