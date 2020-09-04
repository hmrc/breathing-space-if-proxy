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

import play.api.Logging
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.breathingspaceifproxy.HeaderCorrelationId

case class ErrorResponse(value: Future[Result])

object ErrorResponse extends Logging {
  implicit val format: OFormat[Content] = Json.format[Content]

  case class Content(`correlation-id`: Option[String], reason: String)

  def apply(errorCode: Int, reason: String, correlationId: Option[String]): ErrorResponse = {
    logger.error(correlationId.fold(reason)(corrId => s"(Correlation-id: $corrId) $reason"))
    errorResponse(errorCode, reason, correlationId)
  }

  def apply(
    errorCode: Int,
    reasonToLog: String,
    throwable: Throwable,
    correlationId: Option[String]
  ): ErrorResponse = {
    logger.error(correlationId.fold(reasonToLog)(corrId => s"(Correlation-id: $corrId) $reasonToLog"), throwable)
    errorResponse(errorCode, throwable.getMessage, correlationId)
  }

  private def errorResponse(errorCode: Int, reason: String, correlationId: Option[String]): ErrorResponse = {
    val headers = List(CONTENT_TYPE -> MimeTypes.JSON)
    new ErrorResponse(Future.successful {
      Status(errorCode)(Json.toJson(Content(correlationId, reason)))
        .withHeaders(correlationId.fold(headers)(corrId => headers :+ (HeaderCorrelationId -> corrId)): _*)
    })
  }

  // Test Helper
  lazy val correlationIdName =
    classOf[Content].getDeclaredFields
      .map(_.getName)
      .find(_.startsWith("correlation"))
      .get
      .replace("$minus", "-")
}
