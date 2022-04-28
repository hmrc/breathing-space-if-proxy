/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.DebtsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.service.AbstractBaseController
import uk.gov.hmrc.breathingspaceifproxy.model.HttpError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Memorandum_GET
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

@Singleton()
class MemorandumController @Inject()(
  override val appConfig: AppConfig,
  override val auditConnector: AuditConnector,
  override val authConnector: AuthConnector,
  cc: ControllerComponents,
  debtsConnector: DebtsConnector
) extends AbstractBaseController(cc) {

  val action: Option[String] => ActionBuilder[Request, AnyContent] =
    authAction("read:breathing-space-memorandum", _)

  def get(nino: String): Action[Validation[AnyContent]] = action(nino.some).apply(withoutBody) { implicit request =>
    (
      validateHeadersForNPS(BS_Memorandum_GET, debtsConnector.etmpConnector),
      validateNino(nino)
    ).mapN((nino, requestId) => (nino, requestId))
      .fold(HttpError(retrieveCorrelationId, BAD_REQUEST, _).value, _ => MethodNotAllowed(""))
  }
}
