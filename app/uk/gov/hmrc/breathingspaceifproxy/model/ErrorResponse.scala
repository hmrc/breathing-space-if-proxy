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
import play.api.http.{HeaderNames, MimeTypes, Status => HttpStatus}
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.breathingspaceifproxy.Header

case class ErrorResponse(value: Future[Result])

object ErrorResponse extends Logging {

  type Errors = NonEmptyChain[Error]

  def apply(correlationId: => Option[String], httpErrorCode: Int, errors: Errors): ErrorResponse = {
    val payload = Json.obj("errors" -> errors.toChain.toList)
    logger.error(correlationId.fold(payload.toString)(corrId => s"(Correlation-id: $corrId) ${payload.toString}"))
    errorResponse(correlationId, httpErrorCode, payload)
  }

  def apply(correlationId: => Option[String], httpErrorCode: Int, error: Error): ErrorResponse = {
    val payload = Json.obj("errors" -> List(error))
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
    errorResponse(correlationId, HttpStatus.INTERNAL_SERVER_ERROR, payload)
  }

  private def errorResponse(correlationId: Option[String], httpErrorCode: Int, payload: JsObject): ErrorResponse = {
    val headers = List(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
    new ErrorResponse(Future.successful {
      Status(httpErrorCode)(payload)
        .withHeaders(correlationId.fold(headers)(corrId => headers :+ (Header.CorrelationId -> corrId)): _*)
    })
  }
}
