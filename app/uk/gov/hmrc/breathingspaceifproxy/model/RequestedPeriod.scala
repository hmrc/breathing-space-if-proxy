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
import java.time.format.DateTimeFormatter

import play.api.libs.json.{JsObject, Json, Writes}

final case class RequestedPeriod(startDate: LocalDate, endDate: Option[LocalDate], pegaRequestTimestamp: ZonedDateTime)

object RequestedPeriod {

  implicit val reads = Json.reads[RequestedPeriod]

  lazy val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")

  implicit val writes = new Writes[RequestedPeriod] {
    def writes(period: RequestedPeriod): JsObject =
      Json.obj(
        "startDate" -> period.startDate,
        "endDate" -> period.endDate,
        "pegaRequestTimestamp" -> period.pegaRequestTimestamp.format(formatter)
      )
  }
}
