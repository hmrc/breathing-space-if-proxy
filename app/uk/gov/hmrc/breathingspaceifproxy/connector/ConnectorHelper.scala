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

import cats.data._
import cats.syntax.option._
import play.api.Logging
import play.api.http.{HeaderNames, Status => HttpStatus}
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.mvc.Result
import play.api.mvc.Results.Status
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.RESOURCE_NOT_FOUND
import uk.gov.hmrc.http._

// TODO: should not be composing responses to PEGA here in the IF connector!
trait ConnectorHelper extends HttpErrorFunctions with Logging {

  val ifCorrelationIdHeaderName = "CorrelationId"
  val ifRequestTypeHeaderName = "RequestType"
  val ifStaffIdHeaderName = "StaffId"

  def composeResponseFromIF(response: HttpResponse)(implicit url: Url, headerSet: RequiredHeaderSet): Future[Result] = {
    logResponse(response)

    def composeHappyResponse(response: HttpResponse) = Future.successful {
      Status(response.status)(response.body)
        .withHeaders(
          (Header.CorrelationId, headerSet.correlationId.value),
          (HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
        )
        .as(MimeTypes.JSON)
    }

    response.status match {
      case OK => composeHappyResponse(response)

      case CREATED => composeHappyResponse(response)

      case NOT_FOUND =>
        ErrorResponse(headerSet.correlationId.value.some, NOT_FOUND, NonEmptyChain(Error(RESOURCE_NOT_FOUND))).value

      case _ =>
        ErrorResponse(
          headerSet.correlationId.value.some,
          s"Unexpected response status '${response.status}' returned while calling ${url.value}."
        ).value
    }
  }

  def logException(implicit url: Url, headerSet: RequiredHeaderSet): PartialFunction[Throwable, Future[Result]] = {
    case httpException: HttpException =>
      val reasonToLog = s"HTTP error(${httpException.responseCode}) while calling ${url.value}."
      ErrorResponse(headerSet.correlationId.value.some, httpException.responseCode, reasonToLog, httpException).value

    case throwable: Throwable =>
      val reasonToLog = s"Exception caught while calling ${url.value}."
      ErrorResponse(headerSet.correlationId.value.some, HttpStatus.INTERNAL_SERVER_ERROR, reasonToLog, throwable).value
  }

  def logResponse(response: HttpResponse)(implicit url: Url, headerSet: RequiredHeaderSet): Unit =
    response.status match {
      case status if is2xx(status) =>
        logger.debug(
          s"Status($status) for request with ${ifCorrelationIdHeaderName}(${headerSet.correlationId.value}) to ${url.value}"
        )

      case status =>
        logger.error(
          s"ERROR($status) for request ${ifCorrelationIdHeaderName}(${headerSet.correlationId.value}) to ${url.value}"
        )
        logger.debug(s"... with Body: ${response.body}")
    }

  implicit def hc(implicit headerSet: RequiredHeaderSet): HeaderCarrier = {

    val ifRequestHeaders = Seq(
      (ifCorrelationIdHeaderName, headerSet.correlationId.value),
      (ifRequestTypeHeaderName, headerSet.attended.toString),
      (ifStaffIdHeaderName, headerSet.staffId.value)
    )

    // TODO: need to add something like this also:-, authorization = Some(Authorization(bearerToken)))
    HeaderCarrier(extraHeaders = ifRequestHeaders)
  }
}
