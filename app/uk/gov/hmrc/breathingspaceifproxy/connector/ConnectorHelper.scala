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

import scala.concurrent.Future

import cats.syntax.option._
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes, Status => HttpStatus}
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http._

trait ConnectorHelper extends HttpErrorFunctions with Logging {

  def composeResponse(response: HttpResponse)(implicit requestId: UUID, url: Url): Result = {
    logResponse(response)
    Status(response.status)(response.body)
    // the 'withHeaders' is just a memo. To remove from the connector since it will handled by the controller.
      .withHeaders(
        HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
        Header.CorrelationId -> requestId.toString
      )
      .as(MimeTypes.JSON)
  }

  def logException(implicit requestId: UUID, url: Url): PartialFunction[Throwable, Future[Result]] = {
    case httpException: HttpException =>
      val reasonToLog = s"HTTP error(${httpException.responseCode}) while calling ${url.value}."
      ErrorResponse(correlationId, httpException.responseCode, reasonToLog, httpException).value

    case throwable: Throwable =>
      val reasonToLog = s"Exception caught while calling ${url.value}."
      ErrorResponse(correlationId, HttpStatus.INTERNAL_SERVER_ERROR, reasonToLog, throwable).value
  }

  def logResponse(response: HttpResponse)(implicit requestId: UUID, url: Url): Unit =
    response.status match {
      case status if is2xx(status) =>
        logger.debug(s"Status($status) for request(${requestId.toString}) to ${url.value}")

      case status =>
        logger.error(s"ERROR($status) for request(${requestId.toString}) to ${url.value}")
        logger.debug(s"... with Body: ${response.body}")
    }

  private def correlationId(implicit requestId: UUID): Option[String] =
    requestId.toString.some
}
