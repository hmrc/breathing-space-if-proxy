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

import java.util.UUID

import scala.concurrent.Future

import cats.data.NonEmptyChain
import cats.syntax.option._
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.breathingspaceifproxy.Header

case class ErrorResponse(value: Future[Result])

object ErrorResponse extends Logging {

  type Errors = NonEmptyChain[Error]

  def apply(correlationId: => Option[String], httpErrorCode: Int, errors: Errors): ErrorResponse = {
    val payload = Json.obj("errors" -> errors.toChain.toList)
    apply(correlationId, httpErrorCode, payload)
  }

  def apply(correlationId: UUID, errors: Errors): ErrorResponse = {
    val errorList = errors.toChain.toList
    val payload = Json.obj("errors" -> errorList)
    // The HTTP error code is provided by the 1st error item
    apply(correlationId.toString.some, errorList.head.baseError.httpCode, payload)
  }

  def apply(correlationId: UUID, error: Error): ErrorResponse = {
    val payload = Json.obj("errors" -> List(error))
    apply(correlationId.toString.some, error.baseError.httpCode, payload)
  }

  def apply(correlationId: Option[String], httpErrorCode: Int, payload: JsObject): ErrorResponse = {
    val headers = List(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
    new ErrorResponse(Future.successful {
      Status(httpErrorCode)(payload)
        .withHeaders(
          correlationId.fold(headers) { corrId =>
            headers :+ (Header.CorrelationId -> corrId)
          }: _*
        )
    })
  }
}
