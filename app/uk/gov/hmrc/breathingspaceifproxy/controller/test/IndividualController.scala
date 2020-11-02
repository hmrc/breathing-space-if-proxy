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

package uk.gov.hmrc.breathingspaceifproxy.controller.test

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.breathingspaceifproxy.connector.test.IndividualConnector
import uk.gov.hmrc.breathingspaceifproxy.model.HttpError

@Singleton()
class IndividualController @Inject()(
  cc: ControllerComponents,
  individualConnector: IndividualConnector
) extends TestBaseController(cc) {

  val count: Action[AnyContent] = Action.async { implicit request =>
    individualConnector.count
      .map(composeResponse)
      .recover(handleErrorResponse)
  }

  def delete(maybeNino: String): Action[AnyContent] = Action.async { implicit request =>
    validateNino(maybeNino).fold(
      nec => HttpError(retrieveCorrelationId, nec.head).send,
      individualConnector
        .delete(_)
        .map(composeResponse)
        .recover(handleErrorResponse)
    )
  }

  val deleteAll: Action[AnyContent] = Action.async { implicit request =>
    individualConnector.deleteAll
      .map(composeResponse)
      .recover(handleErrorResponse)
  }

  def exists(maybeNino: String): Action[AnyContent] = Action.async { implicit request =>
    validateNino(maybeNino).fold(
      nec => HttpError(retrieveCorrelationId, nec.head).send,
      individualConnector
        .exists(_)
        .map(composeResponse)
        .recover(handleErrorResponse)
    )
  }

  val listOfNinos: Action[AnyContent] = Action.async { implicit request =>
    individualConnector.listOfNinos
      .map(composeResponse)
      .recover(handleErrorResponse)
  }

  val postIndividual: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    individualConnector
      .postIndividual(request.body)
      .map(composeResponse)
      .recover(handleErrorResponse)
  }

  val postIndividuals: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    individualConnector
      .postIndividuals(request.body)
      .map(composeResponse)
      .recover(handleErrorResponse)
  }

  def putIndividual(maybeNino: String): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    validateNino(maybeNino).fold(
      nec => HttpError(retrieveCorrelationId, nec.head).send,
      individualConnector
        .putIndividual(_, request.body)
        .map(composeResponse)
        .recover(handleErrorResponse)
    )
  }
}
