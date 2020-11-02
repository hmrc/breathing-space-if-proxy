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

import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.breathingspaceifproxy.controller.RequestValidation
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorItem, HttpError}
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.SERVER_ERROR
import uk.gov.hmrc.breathingspaceifproxy.model.test.Failures
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

abstract class TestBaseController(cc: ControllerComponents)
    extends BackendController(cc)
    with HttpErrorFunctions
    with RequestValidation {

  def composeResponse(response: HttpResponse): Result = {
    val body =
      if (HttpErrorFunctions.is2xx(response.status)) response.body
      else Json.stringify(Json.toJson(Json.parse(response.body).as[Failures]))

    Status(response.status)(body).as(MimeTypes.JSON)
  }

  def handleErrorResponse(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case UpstreamErrorResponse(message, status, _, _) =>
      logger.error(s"UpstreamErrorResponse with status($status). $message")
      HttpError(retrieveCorrelationId, ErrorItem(SERVER_ERROR)).value

    case throwable: Throwable =>
      logger.error(s"Exception caught for a downstream request. ${throwable.getMessage}")
      HttpError(retrieveCorrelationId, ErrorItem(SERVER_ERROR)).value
  }
}
