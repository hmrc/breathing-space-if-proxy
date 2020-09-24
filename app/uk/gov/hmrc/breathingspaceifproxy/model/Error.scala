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

import enumeratum._
import play.api.libs.json.{JsArray, JsObject, Json, Writes}

sealed abstract class BaseError(val message: String) extends EnumEntry

object BaseError extends Enum[BaseError] {

  case object MISSING_BODY extends BaseError(s"The request must have a body")
  case object MISSING_HEADER extends BaseError(s"Missing required header")
  case object INTERNAL_SERVER_ERROR extends BaseError("Internal server error")
  case object INVALID_HEADER extends BaseError(s"Invalid value for the header")
  case object INVALID_DATE extends BaseError("Invalid date")
  case object INVALID_DATE_RANGE extends BaseError("End-date before start-date")
  case object INVALID_JSON extends BaseError("Payload not in the expected Json format")
  case object INVALID_NINO extends BaseError("Invalid Nino")
  case object RESOURCE_NOT_FOUND extends BaseError("Resource not found")
  case object UNSUPPORTED_MEDIA_TYPE extends BaseError("Content-type should be \"application/json\"")

  override val values = findValues
}

final case class Error(baseError: BaseError, details: Option[String] = None)

object Error {

  implicit val writes = new Writes[Error] {
    def writes(error: Error): JsObject =
      Json.obj(
        "code" -> error.baseError.entryName,
        "message" -> s"${error.baseError.message}${error.details.fold("")(identity)}"
      )
  }

  def fromThrowable(httpErrorCode: Int, throwable: Throwable): JsArray =
    Json.arr(
      Json.obj(
        "code" -> httpErrorIds.getOrElse[String](httpErrorCode, "INTERNAL_SERVER_ERROR"),
        "message" -> throwable.getMessage
      )
    )

  lazy val httpErrorIds = Map[Int, String](
    elems =
      400 -> "BAD_REQUEST",
    401 -> "UNAUTHORIZED",
    402 -> "PAYMENT_REQUIRED",
    403 -> "FORBIDDEN",
    404 -> "RESOURCE_NOT_FOUND",
    405 -> "METHOD_NOT_ALLOWED",
    406 -> "NOT_ACCEPTABLE",
    407 -> "PROXY_AUTHENTICATION_REQUIRED",
    408 -> "REQUEST_TIMEOUT",
    409 -> "CONFLICT",
    410 -> "GONE",
    411 -> "LENGTH_REQUIRED",
    412 -> "PRECONDITION_FAILED",
    413 -> "REQUEST_ENTITY_TOO_LARGE",
    414 -> "REQUEST_URI_TOO_LONG",
    415 -> "UNSUPPORTED_MEDIA_TYPE",
    416 -> "REQUESTED_RANGE_NOT_SATISFIABLE",
    417 -> "EXPECTATION_FAILED",
    422 -> "UNPROCESSABLE_ENTITY",
    423 -> "LOCKED",
    424 -> "FAILED_DEPENDENCY",
    426 -> "UPGRADE_REQUIRED",
    428 -> "PRECONDITION_REQUIRED",
    429 -> "TOO_MANY_REQUESTS",
    500 -> "INTERNAL_SERVER_ERROR",
    501 -> "NOT_IMPLEMENTED",
    502 -> "BAD_GATEWAY",
    503 -> "SERVICE_UNAVAILABLE",
    504 -> "GATEWAY_TIMEOUT",
    505 -> "HTTP_VERSION_NOT_SUPPORTED",
    507 -> "INSUFFICIENT_STORAGE",
    511 -> "NETWORK_AUTHENTICATION_REQUIRED"
  )
}
