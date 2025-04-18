/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.*
import play.api.libs.ws.BodyWritable

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

// --------------------------------------------------------------------------------

final case class PostPeriodInRequest(
  startDate: LocalDate,
  endDate: Option[LocalDate],
  pegaRequestTimestamp: ZonedDateTime
)

object PostPeriodInRequest {
  implicit val reads: Reads[PostPeriodInRequest] = Json.reads[PostPeriodInRequest]

  implicit val writes: Writes[PostPeriodInRequest] = new Writes[PostPeriodInRequest] {
    def writes(postPeriod: PostPeriodInRequest): JsObject = {
      val fields = List(
        startDateKey -> Json.toJson(postPeriod.startDate),
        timestampKey -> JsString(postPeriod.pegaRequestTimestamp.format(timestampFormatter))
      )
      JsObject(postPeriod.endDate.fold(fields)(endDate => fields :+ (endDateKey -> Json.toJson(endDate))))
    }
  }
}

// --------------------------------------------------------------------------------

final case class PostPeriodsInRequest(consumerRequestId: UUID, utr: Option[String], periods: List[PostPeriodInRequest])

object PostPeriodsInRequest {
  implicit val format: OFormat[PostPeriodsInRequest] = Json.format[PostPeriodsInRequest]

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}
