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

package uk.gov.hmrc.breathingspaceifproxy.controller.service

import java.time.ZonedDateTime

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.breathingspaceifproxy.model.{Nino, RequestId, timestampFormatter}
import uk.gov.hmrc.breathingspaceifproxy.model.audit.{AuditDetail, AuditRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

trait Auditing {

  val auditConnector: AuditConnector

  implicit val ec: ExecutionContext

  def auditEvent[R](
    status: Int,
    payload: JsValue
  )(implicit hc: HeaderCarrier, nino: Nino, request: Request[Validation[R]], requestId: RequestId): Future[Unit] =
    Future {
      auditConnector.sendExplicitAudit(
        auditType = requestId.endpointId.auditType,
        AuditDetail(
          correlationId = requestId.correlationId,
          nino = nino.value,
          staffId = requestId.staffId,
          auditRequest,
          auditResponse(status, payload)
        )
      )
    }

  private def auditRequest[R](implicit request: Request[Validation[R]]): JsValue =
    Json.obj(
      "detail"      -> Json.toJson(AuditRequest(request)),
      "generatedAt" -> Json.toJson(ZonedDateTime.now.format(timestampFormatter))
    )

  private def auditResponse[R](status: Int, payload: JsValue): JsValue =
    Json.obj(
      fields = "detail" -> Json.obj(
        "payload"        -> payload,
        "httpStatusCode" -> Json.toJson(status)
      ),
      "generatedAt" -> Json.toJson(ZonedDateTime.now.format(timestampFormatter))
    )

}
