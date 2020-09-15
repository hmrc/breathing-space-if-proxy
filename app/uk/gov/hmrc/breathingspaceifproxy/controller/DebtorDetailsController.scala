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

import scala.concurrent.ExecutionContext

import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.DebtorDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._

@Singleton()
class DebtorDetailsController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  debtorDetailsConnector: DebtorDetailsConnector
)(implicit val ec: ExecutionContext)
    extends BaseController(appConfig, cc) {

  def get(maybeNino: String): Action[AnyContent] = Action.async { implicit request =>
    (
      validateHeaders,
      validateNino(maybeNino)
    ).mapN((_, nino) => nino)
      .fold(
        ErrorResponse(retrieveCorrelationId, BAD_REQUEST, _).value,
        nino => {
          logger.debug(s"Retrieving Debtor's details for Nino(${nino.value})")
          debtorDetailsConnector.get(nino)
        }
      )
  }
}
