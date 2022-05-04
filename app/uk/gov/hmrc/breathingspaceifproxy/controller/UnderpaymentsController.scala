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

import cats.syntax.apply._
import play.api.libs.json.Writes
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.UnderpaymentsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.service.AbstractBaseController
import uk.gov.hmrc.breathingspaceifproxy.model.{HttpError, Nino, RequestId, Underpayments}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Underpayments_GET
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton()
class UnderpaymentsController @Inject()(
  override val appConfig: AppConfig,
  override val auditConnector: AuditConnector,
  override val authConnector: AuthConnector,
  cc: ControllerComponents,
  underpaymentsConnector: UnderpaymentsConnector
) extends AbstractBaseController(cc) {

  val action = authAction("read:breathing-space-debts")

  def get(nino: String, periodId: String): Action[Validation[AnyContent]] =
    enabled(_.underpaymentsFeatureEnabled).andThen(action).async(withoutBody) { implicit request =>
      (
        validateHeadersForNPS(BS_Underpayments_GET, underpaymentsConnector.eisConnector),
        validateNino(nino),
        validatePeriodId(periodId),
        request.body
      ).mapN((requestId, nino, periodId, _) => (requestId, nino, periodId))
        .fold(
          e => {
            logger.error(e.toString)
            HttpError(retrieveCorrelationId, BAD_REQUEST, e).send
          },
          validParams => {
            implicit val (requestId, nino, periodId) = validParams
            logger.debug(s"$requestId for Nino(${nino.value}")
            if (appConfig.onDevEnvironment) logHeaders
            getFromUpstream
          }
        )
    }

  private def getFromUpstream(
    implicit nino: Nino,
    periodId: UUID,
    request: Request[Validation[AnyContent]],
    requestId: RequestId,
    format: Writes[Underpayments]
  ): Future[Result] =
    underpaymentsConnector.get(nino, periodId).flatMap {
      _.fold(auditEventAndSendErrorResponse[AnyContent], auditEventAndSendResponse(OK, _))
    }
}
