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

package uk.gov.hmrc.breathingspaceifproxy.controller

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global

import cats.syntax.apply._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.DebtsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

@Singleton()
class DebtsController @Inject()(
  appConfig: AppConfig,
  auditConnector: AuditConnector,
  cc: ControllerComponents,
  debtsConnector: DebtsConnector
) extends AbstractBaseController(appConfig, auditConnector, cc) {

  def get(maybeNino: String): Action[Validation[AnyContent]] = Action.async(withoutBody) { implicit request =>
    (
      validateHeadersForNPS(BS_Debts_GET),
      validateNino(maybeNino),
      request.body
    ).mapN((requestId, nino, _) => (requestId, nino))
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino) = validationTuple
          logger.debug(s"$requestId for Nino(${nino.value})")
          if (appConfig.onDevEnvironment) logHeaders
          debtsConnector.get(nino).flatMap {
            _.fold(auditEventAndSendErrorResponse[AnyContent], auditEventAndSendResponse(OK, _))
          }
        }
      )
  }
}
