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
import uk.gov.hmrc.breathingspaceifproxy.connector.MemorandumConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.service.AbstractBaseController
import uk.gov.hmrc.breathingspaceifproxy.model.{HttpError, Nino, RequestId}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Memorandum_GET
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

@Singleton()
class MemorandumController @Inject()(
  override val appConfig: AppConfig,
  override val auditConnector: AuditConnector,
  override val authConnector: AuthConnector,
  cc: ControllerComponents,
  memorandumConnector: MemorandumConnector
) extends AbstractBaseController(cc) {

  val action: Option[String] => ActionBuilder[Request, AnyContent] =
    authAction("read:breathing-space-memorandum", _)

  def get(n: Nino): Action[Validation[AnyContent]] =
    enabled(_.memorandumFeatureEnabled)
      .andThen(action(n.value.some))
      .async(withoutBody) { implicit request =>
        (
          validateHeadersForNPS(
            BS_Memorandum_GET,
            memorandumConnector.memorandumConnector
          )
        ).map(requestId => requestId)
          .fold(
            HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
            reqId => {
              implicit val requestId: RequestId = reqId
              implicit val nino: Nino = n
              logger.debug(s"$requestId for Nino(${nino.value})")
              if (appConfig.onDevEnvironment) logHeaders
              memorandumConnector.get(nino).flatMap {
                _.fold(auditEventAndSendErrorResponse[AnyContent], auditEventAndSendResponse(OK, _))
              }
            }
          )
      }
}
