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

import scala.concurrent.Future

import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.RequestId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

abstract class AbstractBaseController(appConfig: AppConfig, cc: ControllerComponents)
    extends BackendController(cc)
    with BaseController
    with RequestValidation {

  def composeResponse[T](status: Int, body: T)(implicit requestId: RequestId, writes: Writes[T]): Future[Result] =
    logAndAddHeaders(Status(status)(Json.toJson(body)))

  def composeResponse(status: Int, response: HttpResponse)(implicit requestId: RequestId): Future[Result] =
    logAndAddHeaders(Status(status)(response.body))

  private def logAndAddHeaders(result: Result)(implicit requestId: RequestId): Future[Result] = {
    logger.debug(s"Response to $requestId has status(${result.header.status})")
    Future.successful {
      result
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
          Header.CorrelationId -> requestId.correlationId.toString
        )
        .as(MimeTypes.JSON)
    }
  }

  override protected implicit def hc(implicit requestFromClient: RequestHeader): HeaderCarrier = {
    val headers = requestFromClient.headers.headers
    // Presence of the headers in "headerMapping" was already validated in RequestValidation
    val extraHeaders = appConfig.headerMapping
      .map { headerMapping =>
        val headerWithValue = headers
          .filter(headerFromClient => headerFromClient._1.toLowerCase == headerMapping.nameToMap.toLowerCase)
          .head

        headerMapping.nameMapped -> headerWithValue._2
      }
      // Remove from extraHeaders the mapped StaffPid when it has as value unattendedStaffPid
      .filter(header => header._1 != appConfig.staffPidMapped || header._2 != unattendedStaffPid)

    super.hc.withExtraHeaders(extraHeaders: _*)
  }
}