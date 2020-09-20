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

package uk.gov.hmrc.breathingspaceifproxy.model

import scala.concurrent.Future

import cats.data.NonEmptyChain
import play.api.Logging
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.{MimeTypes}
import play.api.http.{Status => HttpStatus}
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.INTERNAL_SERVER_ERROR

case class ErrorResponse(value: Future[Result])

object ErrorResponse extends Logging {

  type Errors = NonEmptyChain[Error]

  def apply(correlationId: => Option[String], httpErrorCode: Int, errors: Errors): ErrorResponse = {
    val payload = Json.obj("errors" -> errors.toChain.toList)
    logger.error(correlationId.fold(payload.toString)(corrId => s"(Correlation-id: $corrId) ${payload.toString}"))
    errorResponse(correlationId, httpErrorCode, payload)
  }

  def apply(
    correlationId: => Option[String],
    httpErrorCode: Int,
    reasonToLog: => String,
    throwable: Throwable
  ): ErrorResponse = {
    logger.error(correlationId.fold(reasonToLog)(corrId => s"(Correlation-id: $corrId) $reasonToLog"), throwable)
    val payload = Json.obj("errors" -> Error.fromThrowable(httpErrorCode, throwable))
    errorResponse(correlationId, httpErrorCode, payload)
  }

  def apply(
    correlationId: => Option[String],
    reasonToLog: => String,
    throwable: Option[Throwable] = None
  ): ErrorResponse = {
    logErrorWithMaybeThrowable(
      correlationId.fold(reasonToLog)(corrId => s"(Correlation-id: $corrId) $reasonToLog"),
      throwable
    )
    val payload =
      Json.obj("errors" -> Error(INTERNAL_SERVER_ERROR, Some("An error occurred in the downstream systems")))
    errorResponse(correlationId, HttpStatus.INTERNAL_SERVER_ERROR, payload)
  }

  private def logErrorWithMaybeThrowable(msg: String, maybeThrowable: Option[Throwable]): Unit =
    maybeThrowable.fold(logger.error(msg))(logger.error(msg, _))

  private def errorResponse(correlationId: Option[String], httpErrorCode: Int, payload: JsObject): ErrorResponse = {
    val headers = List(CONTENT_TYPE -> MimeTypes.JSON)
    new ErrorResponse(Future.successful {
      Status(httpErrorCode)(payload)
        .withHeaders(correlationId.fold(headers)(corrId => headers :+ (Header.CorrelationId -> corrId)): _*)
    })
  }
}
