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

import java.util.UUID

import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.validated._
import play.api.Logging
import play.api.http._
import play.api.http.HeaderNames._
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.domain.{Nino => DomainNino}

trait RequestValidation extends Logging {

  def validateHeadersForNPS(implicit request: Request[_]): Validation[UUID] = {
    val headers = request.headers
    (
      validateContentType(request),
      validateCorrelationId(headers),
      validateRequestType(headers),
      validateStaffPid(headers)
    ).mapN((_, correlationId, attended, staffPid) => (correlationId, attended, staffPid))
      .andThen(validateStaffPidForRequestType)
  }

  def validateNino(maybeNino: String): Validation[Nino] =
    if (DomainNino.isValid(maybeNino)) Nino(maybeNino).validNec
    else ErrorItem(INVALID_NINO, s"($maybeNino)".some).invalidNec

  def validateNino(maybeNino: Option[String]): Validation[Nino] =
    maybeNino.fold(ErrorItem(MISSING_NINO).invalidNec[Nino])(validateNino(_))

  def parseJsValue[T](json: JsValue, name: String)(implicit rds: Reads[T]): Option[T] =
    (json \ name).validate[T] match {
      case JsSuccess(value, _) => value.some
      case JsError(_) => none
    }

  def parseJsObject[T](json: JsValue)(implicit rds: Reads[T]): Option[T] =
    json.validate[T] match {
      case JsSuccess(value, _) => value.some
      case JsError(_) => none
    }

  def validateJsArray[T](json: JsArray, name: String, validateItem: (T, Int) => Validation[T])(
    implicit rds: Reads[T]
  ): Validation[List[T]] =
    // MISSING_PERIODS, as error code, is a bit misleading here, since the function is
    // generic and accordingly not thought for checking an array of Periods specifically.
    // It should be then replaced in future with a more generic error code in case we
    // are going to have a different array type.
    if (json.value.isEmpty) ErrorItem(MISSING_PERIODS).invalidNec[List[T]]
    else {
      json.value.zipWithIndex
        .map { jsValueAndIndex =>
          parseJsObject[T](jsValueAndIndex._1)
            .fold {
              ErrorItem(INVALID_JSON_ITEM, s"($name ${jsValueAndIndex._2})".some).invalidNec[T]
            } {
              validateItem(_, jsValueAndIndex._2)
            }
            .map(List(_))
        }
        .reduceLeft(_.combine(_))
    }

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
      .get(Header.CorrelationId)
      .fold[Validation[UUID]] {
        ErrorItem(MISSING_HEADER, s"(${Header.CorrelationId})".some).invalidNec
      } { correlationId =>
        Either
          .catchNonFatal(UUID.fromString(correlationId))
          .fold[Validation[UUID]](
            _ => ErrorItem(INVALID_HEADER, s"(${Header.CorrelationId})".some).invalidNec,
            _.validNec
          )
      }

  private def invalidRequestTypeHeader(requestType: String): Option[String] =
    s"(${Header.RequestType}). Was $requestType but valid values are only: ${Attended.values.mkString(", ")}".some

  private def validateRequestType(headers: Headers): Validation[Attended] =
    headers
      .get(Header.RequestType)
      .fold[Validation[Attended]] {
        ErrorItem(MISSING_HEADER, s"(${Header.RequestType})".some).invalidNec
      } { requestType =>
        Attended
          .withNameOption(requestType.toUpperCase)
          .fold[Validation[Attended]] {
            ErrorItem(INVALID_HEADER, invalidRequestTypeHeader(requestType)).invalidNec
          } { _.validNec }
      }

  private val staffPidRegex = "^[0-9]{7}$".r

  private def validateStaffPid(headers: Headers): Validation[String] =
    headers
      .get(Header.StaffPid)
      .fold[Validation[String]] {
        ErrorItem(MISSING_HEADER, s"(${Header.StaffPid})".some).invalidNec
      } { staffPid =>
        staffPidRegex
          .findFirstIn(staffPid)
          .fold[Validation[String]] {
            ErrorItem(INVALID_HEADER, s"(${Header.StaffPid}). Expected a 7-digit number but was $staffPid".some).invalidNec
          } { _.validNec }
      }

  private def validateStaffPidForRequestType(headerValues: (UUID, Attended, String)): Validation[UUID] = {
    val requestType = headerValues._2
    val staffPid = headerValues._3
    if (requestType == Attended.DS2_BS_ATTENDED && staffPid != unattendedStaffPid
      || requestType == Attended.DS2_BS_UNATTENDED && staffPid == unattendedStaffPid) {
      // correlationId as UUID
      headerValues._1.validNec[ErrorItem]
    } else {
      ErrorItem(INVALID_HEADER, s"(${Header.StaffPid}). Cannot be '$staffPid' for $requestType".some).invalidNec[UUID]
    }
  }

  def retrieveCorrelationId(implicit request: Request[_]): Option[String] =
    request.headers.get(Header.CorrelationId)
}
