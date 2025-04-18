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

final case class PutPeriodInRequest(
  periodID: UUID,
  startDate: LocalDate,
  endDate: Option[LocalDate],
  pegaRequestTimestamp: ZonedDateTime
)

object PutPeriodInRequest {
  implicit val reads: Reads[PutPeriodInRequest] = Json.reads[PutPeriodInRequest]

  implicit val writes: Writes[PutPeriodInRequest] = new Writes[PutPeriodInRequest] {
    def writes(putPeriod: PutPeriodInRequest): JsObject = {
      val fields = List(
        periodIdKey  -> Json.toJson(putPeriod.periodID),
        startDateKey -> Json.toJson(putPeriod.startDate),
        timestampKey -> JsString(putPeriod.pegaRequestTimestamp.format(timestampFormatter))
      )
      JsObject(putPeriod.endDate.fold(fields)(endDate => fields :+ (endDateKey -> Json.toJson(endDate))))
    }
  }
}

final case class PutPeriodsInRequest(periods: List[PutPeriodInRequest])

object PutPeriodsInRequest {
  implicit val format: OFormat[PutPeriodsInRequest] = Json.format[PutPeriodsInRequest]

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}
