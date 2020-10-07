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

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import play.api.libs.json.{JsObject, Json, Writes}

// --------------------------------------------------------------------------------

trait PeriodInRequest {
  val startDate: LocalDate
  val endDate: Option[LocalDate]
  val pegaRequestTimestamp: ZonedDateTime
}

// --------------------------------------------------------------------------------

final case class PostPeriodInRequest(
  startDate: LocalDate,
  endDate: Option[LocalDate],
  pegaRequestTimestamp: ZonedDateTime
) extends PeriodInRequest

object PostPeriodInRequest {
  implicit val reads = Json.reads[PostPeriodInRequest]

  implicit val writes = new Writes[PostPeriodInRequest] {
    def writes(postPeriod: PostPeriodInRequest): JsObject =
      Json.obj(
        startDateKey -> postPeriod.startDate,
        endDateKey -> postPeriod.endDate,
        timestampKey -> postPeriod.pegaRequestTimestamp.format(timestampFormatter)
      )
  }
}

// --------------------------------------------------------------------------------

final case class PutPeriodInRequest(
  periodID: UUID,
  startDate: LocalDate,
  endDate: Option[LocalDate],
  pegaRequestTimestamp: ZonedDateTime
) extends PeriodInRequest

object PutPeriodInRequest {
  implicit val reads = Json.reads[PutPeriodInRequest]

  implicit val writes = new Writes[PutPeriodInRequest] {
    def writes(putPeriod: PutPeriodInRequest): JsObject =
      Json.obj(
        periodIdKey -> putPeriod.periodID,
        startDateKey -> putPeriod.startDate,
        endDateKey -> putPeriod.endDate,
        timestampKey -> putPeriod.pegaRequestTimestamp.format(timestampFormatter)
      )
  }
}
