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
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorResponse, RequestId}
import uk.gov.hmrc.breathingspaceifproxy.model.EndpointId.Breathing_Space_Individual_Details_GET

@Singleton()
class IndividualDetailsController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  individualDetailsConnector: IndividualDetailsConnector
) extends AbstractBaseController(appConfig, cc) {

  def getMinimalPopulation(maybeNino: String): Action[Validation[AnyContent]] = Action.async(withoutBody) {
    implicit request =>
      (
        validateHeadersForNPS,
        validateNino(maybeNino),
        request.body
      ).mapN((correlationId, nino, _) => (RequestId(Breathing_Space_Individual_Details_GET, correlationId), nino))
        .fold(
          ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
          validationTuple => {
            implicit val (requestId, nino) = validationTuple
            logger.debug(s"$requestId for Nino(${nino.value})")
            individualDetailsConnector.getMinimalPopulation(nino).flatMap {
              _.fold(ErrorResponse(requestId.correlationId, _).value, composeResponse(OK, _))
            }
          }
        )
  }
}