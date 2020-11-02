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

package uk.gov.hmrc.breathingspaceifproxy.model.test

import play.api.libs.json.{JsObject, Json, Writes}

final case class Failure(code: String, reason: String)
object Failure {
  implicit val reads = Json.reads[Failure]

  implicit val writes = new Writes[Failure] {
    def writes(failure: Failure): JsObject =
      Json.obj(fields = "code" -> failure.code, "message" -> failure.reason)
  }
}

final case class Failures(failures: List[Failure])
object Failures {
  implicit val reads = Json.reads[Failures]

  implicit val writes = new Writes[Failures] {
    def writes(failures: Failures): JsObject =
      Json.obj(fields = "errors" -> failures.failures)
  }
}
