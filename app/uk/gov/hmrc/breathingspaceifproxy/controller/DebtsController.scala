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

package uk.gov.hmrc.breathingspaceifproxy.controller

import javax.inject.{Inject, Singleton}

import cats.syntax.apply._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.DebtsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.service.AbstractBaseController
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

@Singleton()
class DebtsController @Inject()(
  override val appConfig: AppConfig,
  override val auditConnector: AuditConnector,
  override val authConnector: AuthConnector,
  cc: ControllerComponents,
  debtsConnector: DebtsConnector
) extends AbstractBaseController(cc) {

  val action = authAction("read:breathing-space-debts")

  def get(maybeNino: String, maybePeriodId: String): Action[Validation[AnyContent]] = action.async(withoutBody) {
    implicit request =>
      (
        validateHeadersForNPS(BS_Debts_GET, debtsConnector.etmpConnector),
        validateNino(maybeNino),
        validatePeriodId(maybePeriodId),
        request.body
      ).mapN((requestId, nino, periodId, _) => (requestId, nino, periodId))
        .fold(
          HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
          validationTuple => {
            implicit val (requestId, nino, periodId) = validationTuple
            logHeadersAndRequestId(nino, requestId)
            debtsConnector.get(nino, periodId).flatMap {
              _.fold(auditEventAndSendErrorResponse[AnyContent], auditEventAndSendResponse(OK, _))
            }
          }
        )
  }

}
