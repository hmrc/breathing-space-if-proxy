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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.either._
import cats.syntax.validated._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.{AppConfig, HeaderMapping}
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

abstract class AbstractBaseController(appConfig: AppConfig, cc: ControllerComponents)
    extends BackendController(cc)
    with RequestValidation {

  val withoutBody: BodyParser[Validation[AnyContent]] = BodyParser("Breathing-Space-without-Body") { request =>
    if (request.hasBody) errorOnBody(INVALID_BODY)(request) else parse.ignore(AnyContent().validNec)(request)
  }

  val withJsonBody: BodyParser[Validation[JsValue]] = BodyParser("Breathing-Space-with-Body") { request =>
    if (request.hasBody) parseBodyAsJson(request) else errorOnBody(MISSING_BODY)(request)
  }

  private val parseBodyAsJson: BodyParser[Validation[JsValue]] =
    parse.tolerantText.map { text =>
      Either
        .catchNonFatal(Json.parse(text))
        .fold[Validation[JsValue]](_ => ErrorItem(INVALID_JSON).invalidNec, _.validNec)
    }

  private def errorOnBody[T](error: BaseError): BodyParser[Validation[T]] =
    parse.ignore[Validation[T]](ErrorItem(error).invalidNec)

  def composeResponse[T](status: Int, body: T)(implicit requestId: RequestId, writes: Writes[T]): Future[Result] =
    logAndAddHeaders(Status(status)(Json.toJson(body)))

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
    val headers = requestFromClient.headers
      .replace(List(Header.Authorization -> appConfig.integrationframeworkAuthToken): _*)

    // Presence of the headers to map (by using "appConfig.headerMapping") was already validated
    // in RequestValidation. The mapping only takes place on the header's name, not on the value.
    val extraHeaders =
      (Header.Environment -> appConfig.integrationFrameworkEnvironment) +: appConfig.headerMapping
        .map(mapAndReturnHeader(_, headers.headers))
        .filter(header => header._1 != appConfig.staffPidMapped || header._2 != unattendedStaffPid)

    HeaderCarrierConverter
      .fromHeadersAndSessionAndRequest(headers, request = Some(requestFromClient))
      .withExtraHeaders(extraHeaders: _*)
  }

  private def mapAndReturnHeader(hdrMapping: HeaderMapping, hdrs: Seq[(String, String)]): (String, String) = {
    val hdrValue =
      hdrs.filter(hdrFromClient => hdrFromClient._1.toLowerCase == hdrMapping.nameToMap.toLowerCase).head._2
    hdrMapping.nameMapped -> hdrValue
  }
}
