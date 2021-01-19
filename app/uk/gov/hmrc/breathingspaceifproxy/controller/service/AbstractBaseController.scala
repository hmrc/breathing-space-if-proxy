/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.breathingspaceifproxy.controller.service

import scala.concurrent.Future

import cats.syntax.either._
import cats.syntax.validated._
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.{AppConfig, HeaderMapping}
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

abstract class AbstractBaseController(
  override val controllerComponents: ControllerComponents
) extends BackendController(controllerComponents)
    with Auditing
    with Helpers
    with Logging
    with RequestAuth
    with RequestValidation {

  val appConfig: AppConfig

  implicit val ec = controllerComponents.executionContext

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

  def auditEventAndSendErrorResponse[R](
    errors: Errors
  )(implicit nino: Nino, request: Request[Validation[R]], requestId: RequestId): Future[Result] = {
    val errorList = errors.toChain.toList
    val payload = Json.obj("errors" -> errorList)
    auditEvent(errorList.head.baseError.httpCode, payload)
    Future.successful {
      HttpError(requestId.correlationId, errors).value
        .withHeaders(Header.UpstreamState -> requestId.upstreamConnector.currentState)
    }
  }

  def auditEventAndSendResponse[R, T](
    status: Int,
    body: T
  )(implicit nino: Nino, request: Request[Validation[R]], requestId: RequestId, writes: Writes[T]): Future[Result] = {
    val payload = Json.toJson(body)
    auditEvent(status, payload)
    sendResponse(status, payload)
  }

  def logHeaders(implicit request: RequestHeader): Unit =
    logger.info(request.headers.headers.toList.mkString("Headers[", ":", "]"))

  private def errorOnBody[T](error: BaseError): BodyParser[Validation[T]] =
    parse.ignore[Validation[T]](ErrorItem(error).invalidNec)

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

  private def sendResponse[T](status: Int, payload: JsValue)(implicit requestId: RequestId): Future[Result] = {
    logger.debug(s"Response to $requestId has status(${status})")
    Future.successful {
      Status(status)(payload)
        .withHeaders(
          HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
          Header.CorrelationId -> requestId.correlationId.toString,
          Header.UpstreamState -> requestId.upstreamConnector.currentState
        )
        .as(MimeTypes.JSON)
    }
  }
}
