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

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model.{Attended, ErrorResponse, Nino}
import uk.gov.hmrc.domain.{Nino => DomainNino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

trait BaseController extends BackendController with Logging {

  type MissingHeaders = Option[ErrorResponse]
  type ValidNino = Either[ErrorResponse, Nino]

  def vouchRequiredHeaders(implicit request: Request[_]): MissingHeaders = {
    val headers = request.headers
    val context = headers.get(HeaderContext)
    val correlationId = headers.get(HeaderCorrelationId)

    if (correlationId.isEmpty || context.isEmpty) {
      Some(ErrorResponse(BAD_REQUEST, MissingRequiredHeaders, correlationId))
    } else {
      Attended
        .withNameOption(context.get)
        .fold[MissingHeaders] {
          val errorResponse = ErrorResponse(BAD_REQUEST, invalidContextHeader(context.get), correlationId)
          Some(errorResponse)
        } { _ =>
          None
        }
    }
  }

  def vouchValidNino(nino: String)(implicit hc: HeaderCarrier): ValidNino =
    if (DomainNino.isValid(nino)) Right(Nino(nino))
    else Left(ErrorResponse(UNPROCESSABLE_ENTITY, invalidNino(nino), retrieveCorrelationId))
}
