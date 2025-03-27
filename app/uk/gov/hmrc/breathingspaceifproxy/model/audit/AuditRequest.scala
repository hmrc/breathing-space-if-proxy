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

package uk.gov.hmrc.breathingspaceifproxy.model.audit

import cats.syntax.option._
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.breathingspaceifproxy.Validation

case class AuditRequest(
  method: String,
  path: String,
  payload: Option[JsValue]
)

object AuditRequest {
  implicit val writes: OWrites[AuditRequest] = Json.writes[AuditRequest]

  def apply[R](request: Request[Validation[R]]): AuditRequest =
    AuditRequest(
      method = request.method,
      path = request.path,
      payload = request.body.fold(
        _ => none,
        {
          case b: JsValue => b.some
          case _          => none
        }
      )
    )
}
