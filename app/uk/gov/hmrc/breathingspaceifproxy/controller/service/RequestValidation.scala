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

package uk.gov.hmrc.breathingspaceifproxy.controller.service

import java.util.UUID
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.validated._
import play.api.http._
import play.api.http.HeaderNames._
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.connector.service.UpstreamConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.{Attended, EndpointId}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.{
  BS_Memorandum_GET,
  BS_Periods_POST,
  BS_Periods_PUT,
  BS_Underpayments_GET
}

import java.lang.Integer.parseInt

trait RequestValidation {

  def validateHeadersForNPS(endpointId: EndpointId, upstreamConnector: UpstreamConnector)(
    implicit request: Request[_]
  ): Validation[RequestId] = {
    val headers = request.headers
    (
      validateContentType(request),
      validateCorrelationId(headers),
      validateRequestType(headers, endpointId),
      validateStaffPid(headers, endpointId)
    ).mapN((_, correlationId, attended, staffPid) => (correlationId, attended, staffPid))
      .andThen(validateStaffPidForRequestType(endpointId, upstreamConnector))
  }

  def validateNino(maybeNino: String): Validation[Nino] =
    Nino
      .fromString(maybeNino)
      .fold(ErrorItem(INVALID_NINO, s"($maybeNino)".some).invalidNec[Nino])(_.validNec[ErrorItem])

  def validateNino(maybeNino: Option[String]): Validation[Nino] =
    maybeNino.fold(ErrorItem(MISSING_NINO).invalidNec[Nino])(validateNino(_))

  def parseJsValue[T](json: JsValue, name: String)(implicit rds: Reads[T]): Option[T] =
    (json \ name).validate[T] match {
      case JsSuccess(value, _) => value.some
      case JsError(_) => none
    }

  def parseJsValueOpt[T](json: JsValue, name: String)(implicit rds: Reads[T]): Validation[Option[T]] =
    (json \ name).validateOpt[T] match {
      case JsSuccess(value, _) => value.validNec
      case JsError(_) => ErrorItem(INVALID_JSON, s" ($name)".some).invalidNec
    }

  def parseJsObject[T](json: JsValue)(implicit rds: Reads[T]): Option[T] =
    json.validate[T] match {
      case JsSuccess(value, _) => value.some
      case JsError(_) => none
    }

  def validateJsArray[T](json: JsArray, name: String)(
    implicit rds: Reads[T]
  ): Validation[List[T]] =
    // MISSING_PERIODS, as error code, is a bit misleading here, since the function is
    // generic and accordingly not thought for checking an array of Periods specifically.
    // It should be then replaced in future with a more generic error code in case we
    // are going to have another array type in addition to Periods.
    if (json.value.isEmpty) ErrorItem(MISSING_PERIODS).invalidNec[List[T]]
    else {
      json.value.zipWithIndex
        .map { jsValueAndIndex =>
          parseJsObject[T](jsValueAndIndex._1)
            .fold(ErrorItem(INVALID_JSON_ITEM, s"($name ${jsValueAndIndex._2})".some).invalidNec[T])(_.validNec)
            .map(List(_))
        }
        .reduceLeft(_.combine(_))
    }

  def validatePeriodId(maybePeriodId: String): Validation[UUID] =
    Either
      .catchNonFatal(UUID.fromString(maybePeriodId))
      .fold(
        _ => ErrorItem(INVALID_PERIOD_ID, s"(${DownstreamHeader.CorrelationId})".some).invalidNec[UUID],
        _.validNec[ErrorItem]
      )

  private def validateContentType(request: Request[_]): Validation[Unit] =
    request.headers
      .get(CONTENT_TYPE)
      .fold[Validation[Unit]] {
        if (request.method.toUpperCase == HttpVerbs.GET) unit.validNec
        // The "Content-type" header is mandatory for POST, PUT and DELETE,
        // (they are the only HTTP methods accepted, with GET of course).
        else ErrorItem(MISSING_HEADER, s"(${CONTENT_TYPE})".some).invalidNec
      } { contentType =>
        // In case the "Content-type" header is specified, a body,
        // if any, is always expected to be in Json format.
        if (contentType.toLowerCase == MimeTypes.JSON.toLowerCase) unit.validNec
        else ErrorItem(INVALID_HEADER, s"(${CONTENT_TYPE}). Invalid value: ${contentType}".some).invalidNec
      }

  private def validateCorrelationId(headers: Headers): Validation[UUID] =
    headers
      .get(DownstreamHeader.CorrelationId)
      .fold[Validation[UUID]] {
        ErrorItem(MISSING_HEADER, s"(${DownstreamHeader.CorrelationId})".some).invalidNec
      } { correlationId =>
        Either
          .catchNonFatal(UUID.fromString(correlationId))
          .fold[Validation[UUID]](
            _ => ErrorItem(INVALID_HEADER, s"(${DownstreamHeader.CorrelationId})".some).invalidNec,
            _.validNec
          )
      }

  private def illegalRequestTypeHeader(requestType: String): Option[String] =
    s"(${DownstreamHeader.RequestType}). $requestType is an illegal value for this endpoint".some

  private def invalidRequestTypeHeader(requestType: String): Option[String] =
    s"(${DownstreamHeader.RequestType}). Was $requestType but valid values are only: ${Attended.values.mkString(", ")}".some

  private def validateRequestType(headers: Headers, endpointId: EndpointId): Validation[Attended] =
    headers
      .get(DownstreamHeader.RequestType)
      .fold[Validation[Attended]] {
        if (endpointId == BS_Memorandum_GET) {
          Attended.DA2_PTA.validNec
        } else {
          ErrorItem(MISSING_HEADER, s"(${DownstreamHeader.RequestType})".some).invalidNec
        }
      } { requestType =>
        Attended
          .withNameOption(requestType.toUpperCase)
          .fold[Validation[Attended]] {
            ErrorItem(INVALID_HEADER, invalidRequestTypeHeader(requestType)).invalidNec
          } {
            case Attended.DA2_BS_ATTENDED
                if endpointId == BS_Periods_POST || endpointId == BS_Periods_PUT || endpointId == BS_Underpayments_GET =>
              ErrorItem(INVALID_HEADER, illegalRequestTypeHeader(requestType)).invalidNec

            case attended =>
              if (endpointId == BS_Memorandum_GET) {
                ErrorItem(INVALID_HEADER, illegalRequestTypeHeader(requestType)).invalidNec
              }
              attended.validNec
          }
      }

  private val staffPidRegex = "^[0-9]{7}$".r

  private def validateStaffPid(headers: Headers, endpointId: EndpointId): Validation[String] =
    headers
      .get(DownstreamHeader.StaffPid)
      .fold[Validation[String]] {
        if (endpointId == BS_Memorandum_GET) {
          unattendedStaffPid.validNec
        } else {
          ErrorItem(MISSING_HEADER, s"(${DownstreamHeader.StaffPid})".some).invalidNec
        }
      } { staffPid =>
        if (endpointId == BS_Memorandum_GET)
          ErrorItem(
            INVALID_HEADER,
            s"(${DownstreamHeader.StaffPid}). Cannot be '$staffPid' for Breathing Space Status".some
          ).invalidNec
        else {
          staffPidRegex
            .findFirstIn(staffPid)
            .fold[Validation[String]] {
              ErrorItem(
                INVALID_HEADER,
                s"(${DownstreamHeader.StaffPid}). Expected a 7-digit number but was $staffPid".some
              ).invalidNec
            } {
              _.validNec
            }
        }
      }

  private def validateStaffPidForRequestType(
    endpointId: EndpointId,
    upstreamConnector: UpstreamConnector
  )(headerValues: (UUID, Attended, String)): Validation[RequestId] = {
    val requestType = headerValues._2
    val staffPid = headerValues._3
    if (requestType == Attended.DA2_BS_ATTENDED && staffPid != unattendedStaffPid
      || requestType == Attended.DA2_BS_UNATTENDED && staffPid == unattendedStaffPid
      || requestType == Attended.DA2_PTA && staffPid == unattendedStaffPid) {
      RequestId(endpointId, correlationId = headerValues._1, requestType, staffPid, upstreamConnector)
        .validNec[ErrorItem]
    } else {
      ErrorItem(
        INVALID_HEADER,
        s"(${DownstreamHeader.StaffPid}). Cannot be '$staffPid' for ${requestType.entryName}".some
      ).invalidNec[RequestId]
    }
  }
}
