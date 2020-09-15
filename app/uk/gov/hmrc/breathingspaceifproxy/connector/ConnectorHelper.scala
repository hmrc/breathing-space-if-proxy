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

import play.api.Logging
import play.api.http.{Status => HttpStatus}
import play.api.mvc.Result
import play.api.mvc.Results.Status
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http._

trait ConnectorHelper extends HttpErrorFunctions with Logging {

  def composeResponseFromIF(response: HttpResponse)(implicit url: Url, hc: HeaderCarrier): Result = {
    logResponse(response)
    Status(response.status)(response.body)
      .withHeaders(response.headers.toList.map(header => (header._1, header._2.mkString(""))): _*)
      .as(MimeTypes.JSON)
  }

  def logException(implicit url: Url, hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case httpException: HttpException =>
      val reasonToLog = s"HTTP error(${httpException.responseCode}) while calling ${url.value}."
      ErrorResponse(retrieveCorrelationId, httpException.responseCode, reasonToLog, httpException).value

    case throwable: Throwable =>
      val reasonToLog = s"Exception caught while calling ${url.value}."
      ErrorResponse(retrieveCorrelationId, HttpStatus.INTERNAL_SERVER_ERROR, reasonToLog, throwable).value
  }

  def logResponse(response: HttpResponse)(implicit url: Url, hc: HeaderCarrier): Unit =
    response.status match {
      case status if is2xx(status) =>
        logger.debug(s"Status($status) for request ${request(retrieveCorrelationId)}to ${url.value}")

      case status =>
        logger.error(s"ERROR($status) for request ${request(retrieveCorrelationId)}to ${url.value}")
        logger.debug(s"... with Body: ${response.body}")
    }

  private def request(correlationId: Option[String]): String =
    correlationId.fold("")(value => s"with ${Header.CorrelationId}($value) ")
}
