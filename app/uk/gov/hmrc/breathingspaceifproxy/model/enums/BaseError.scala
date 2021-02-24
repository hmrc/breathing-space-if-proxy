/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.breathingspaceifproxy.model.enums

import enumeratum._
import play.api.http.Status._

sealed abstract class BaseError(val httpCode: Int, val message: String) extends EnumEntry

object BaseError extends Enum[BaseError] {

  case object CONFLICTING_REQUEST
      extends BaseError(
        CONFLICT,
        "The upstream service has indicated that the request is conflicting. Maybe a duplicate POST?"
      )

  case object BREATHING_SPACE_EXPIRED extends BaseError(FORBIDDEN, "Breathing Space has expired for the given Nino")
  case object INVALID_BODY extends BaseError(BAD_REQUEST, "Not expected a body to this endpoint")
  case object INVALID_CONSUMER_REQUEST_ID extends BaseError(BAD_REQUEST, "Invalid consumerRequestId format")
  case object INVALID_ENDPOINT extends BaseError(BAD_REQUEST, "Not a valid endpoint")
  case object INVALID_HEADER extends BaseError(BAD_REQUEST, "Invalid value for the header")
  case object INVALID_JSON extends BaseError(BAD_REQUEST, "Payload not in the expected Json format")
  case object INVALID_JSON_ITEM extends BaseError(BAD_REQUEST, "One or more values cannot validated for the Json item")
  case object INVALID_NINO extends BaseError(BAD_REQUEST, "Invalid Nino format")
  case object INVALID_UTR extends BaseError(BAD_REQUEST, "Invalid UTR format")
  case object MISSING_BODY extends BaseError(BAD_REQUEST, "The request must have a body")

  case object MISSING_CONSUMER_REQUEST_ID
      extends BaseError(BAD_REQUEST, "Payload does not contain a 'consumerRequestId' value?")

  case object MISSING_HEADER extends BaseError(BAD_REQUEST, "Missing required header")
  case object MISSING_NINO extends BaseError(BAD_REQUEST, "Payload does not contain a 'nino' value?")
  case object MISSING_PERIODS extends BaseError(BAD_REQUEST, "Payload does not contain a 'periods' array?")
  case object NO_DATA_FOUND extends BaseError(NOT_FOUND, "No records found for the given Nino")
  case object NOT_AUTHORISED extends BaseError(UNAUTHORIZED, "Request is not authorised. ")
  case object NOT_IN_BREATHING_SPACE extends BaseError(NOT_FOUND, "The given Nino is not in Breathing Space")

  case object RESOURCE_NOT_FOUND
      extends BaseError(NOT_FOUND, "The upstream service has indicated that the provided resource was not found")

  case object SERVER_ERROR
      extends BaseError(
        INTERNAL_SERVER_ERROR,
        "We are currently experiencing problems that require live service intervention"
      )

  case object UPSTREAM_BAD_GATEWAY extends BaseError(INTERNAL_SERVER_ERROR, "The upstream service is not responding")
  case object UPSTREAM_SERVICE_UNAVAILABLE
      extends BaseError(INTERNAL_SERVER_ERROR, "The upstream service is unavailable")
  case object UPSTREAM_TIMEOUT extends BaseError(INTERNAL_SERVER_ERROR, "Request timed out")

  override val values = findValues
}
