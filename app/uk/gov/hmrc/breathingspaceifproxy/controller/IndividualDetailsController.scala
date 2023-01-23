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
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.service.AbstractBaseController
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

@Singleton()
class IndividualDetailsController @Inject()(
  override val appConfig: AppConfig,
  override val auditConnector: AuditConnector,
  override val authConnector: AuthConnector,
  cc: ControllerComponents,
  individualDetailsConnector: IndividualDetailsConnector
) extends AbstractBaseController(cc) {

  val action = authAction("read:breathing-space-individual")

  def getDetails(maybeNino: String): Action[Validation[AnyContent]] = action.async(withoutBody) { implicit request =>
    (
      validateHeadersForNPS(BS_Details_GET, individualDetailsConnector.eisConnector),
      validateNino(maybeNino),
      request.body
    ).mapN((requestId, nino, _) => (requestId, nino))
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino) = validationTuple
          logHeadersAndRequestId(nino, requestId)
          individualDetailsConnector.getDetails(nino).flatMap {
            _.fold(auditEventAndSendErrorResponse[AnyContent], auditEventAndSendResponse(OK, _))
          }
        }
      )
  }
}
