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
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Result
import play.api.mvc.Results.Status

case class ErrorResponse(value: Future[Result])

object ErrorResponse extends Logging {
  implicit val format: OFormat[ErrorValues] = Json.format[ErrorValues]

  case class ErrorValues(`correlation-id`: Option[String], reason: String)

  def apply(errorCode: Int, reason: String, correlationId: Option[String]): ErrorResponse = {
    logger.error(correlationId.fold(reason)(corrId => s"(Correlation-id: $corrId) $reason"))
    new ErrorResponse(Future.successful {
      Status(errorCode)(Json.toJson(ErrorValues(correlationId, reason)))
    })
  }
}
