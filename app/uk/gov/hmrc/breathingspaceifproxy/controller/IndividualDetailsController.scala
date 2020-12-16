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
import scala.concurrent.Future

import cats.syntax.apply._
import play.api.mvc._
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

@Singleton()
class IndividualDetailsController @Inject()(
  appConfig: AppConfig,
  auditConnector: AuditConnector,
  cc: ControllerComponents,
  individualDetailsConnector: IndividualDetailsConnector
) extends AbstractBaseController(appConfig, auditConnector, cc) {

  def getDetail0(maybeNino: String): Action[Validation[AnyContent]] = Action.async(withoutBody) { implicit request =>
    getIndividualDetails(maybeNino, BS_Detail0_GET)
  }

  def getDetails(maybeNino: String): Action[Validation[AnyContent]] = Action.async(withoutBody) { implicit request =>
    getIndividualDetails(maybeNino, BS_Details_GET)
  }

  private def getIndividualDetails(maybeNino: String, endpointId: EndpointId)(
    implicit request: Request[Validation[AnyContent]]
  ): Future[Result] =
    (
      validateHeadersForNPS(endpointId),
      validateNino(maybeNino),
      request.body
    ).mapN((requestId, nino, _) => (requestId, nino))
      .fold(
        HttpError(retrieveCorrelationId, BAD_REQUEST, _).send,
        validationTuple => {
          implicit val (requestId, nino) = validationTuple
          logger.debug(s"$requestId for Nino(${nino.value})")
          if (appConfig.onDevEnvironment) logHeaders

          if (endpointId == BS_Details_GET) {
            individualDetailsConnector.getDetails(nino).flatMap {
              _.fold(auditEventAndSendErrorResponse[AnyContent], auditEventAndSendResponse(OK, _))
            }
          } else {
            individualDetailsConnector.getDetail0(nino).flatMap {
              _.fold(auditEventAndSendErrorResponse[AnyContent], auditEventAndSendResponse(OK, _))
            }
          }
        }
      )
}
