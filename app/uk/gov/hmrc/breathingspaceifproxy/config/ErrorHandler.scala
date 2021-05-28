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

package uk.gov.hmrc.breathingspaceifproxy.config

import javax.inject.{Inject, Provider}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api._
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import uk.gov.hmrc.breathingspaceifproxy.DownstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.{INVALID_ENDPOINT, SERVER_ERROR}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

class ErrorHandler @Inject()(
  routerProvider: Provider[Router],
  auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent,
  configuration: Configuration
) extends JsonErrorHandler(auditConnector, httpAuditEvent, configuration)
    with Logging {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val correlationId = request.headers.get(DownstreamHeader.CorrelationId)
    val endpoint = s"${request.method} ${request.path} has status code"
    if (statusCode == NOT_FOUND) sendInvalidEndpoint(correlationId, endpoint)
    else {
      logger.error(s"""${logCorrelationId(correlationId)} $endpoint($statusCode). Client error was: "$message"""")
      val payload = Json.obj("errors" -> listWithOneError(statusCode, removeCodeDetailIfAny(message)))
      HttpError(correlationId, statusCode, payload).send
    }
  }

  override def onServerError(request: RequestHeader, throwable: Throwable): Future[Result] = {
    val correlationId = request.headers.get(DownstreamHeader.CorrelationId)
    val endpoint = s"${request.method} ${request.path}"
    logger.error(s"${logCorrelationId(correlationId)} $endpoint", throwable)
    HttpError(correlationId, ErrorItem(SERVER_ERROR)).send
  }

  private def logCorrelationId(correlationId: Option[String]): String =
    s"(Correlation-id: ${correlationId.fold("not-provided")(identity)})"

  private def removeCodeDetailIfAny(message: String): String = {
    val ix = message.indexOf("\n")
    message.substring(0, if (ix == -1) message.length else ix)
  }

  private def sendInvalidEndpoint(correlationId: Option[String], endpoint: String): Future[Result] = {
    val endpoints = routerProvider.get.documentation.map { route =>
      val method = route._1
      val path = route._2.replace("/$", "/:")
      s"\t$method\t$path\n"
    }.mkString

    val error = s"${logCorrelationId(correlationId)} $endpoint(INVALID_ENDPOINT). Available endpoints are: \n$endpoints"
    logger.error(error)
    HttpError(correlationId, ErrorItem(INVALID_ENDPOINT)).send
  }

  private def listWithOneError(statusCode: Int, message: String): JsArray =
    Json.arr(
      Json.obj(
        "code" -> httpErrorCodes.getOrElse[String](statusCode, "SERVER_ERROR"),
        "message" -> message
      )
    )

  lazy val httpErrorCodes = Map[Int, String](
    400 -> "BAD_REQUEST",
    401 -> "UNAUTHORIZED",
    402 -> "PAYMENT_REQUIRED",
    403 -> "FORBIDDEN",
    404 -> "RESOURCE_NOT_FOUND",
    405 -> "METHOD_NOT_ALLOWED",
    406 -> "NOT_ACCEPTABLE",
    407 -> "PROXY_AUTHENTICATION_REQUIRED",
    408 -> "REQUEST_TIMEOUT",
    409 -> "CONFLICT",
    410 -> "GONE",
    411 -> "LENGTH_REQUIRED",
    412 -> "PRECONDITION_FAILED",
    413 -> "REQUEST_ENTITY_TOO_LARGE",
    414 -> "REQUEST_URI_TOO_LONG",
    415 -> "UNSUPPORTED_MEDIA_TYPE",
    416 -> "REQUESTED_RANGE_NOT_SATISFIABLE",
    417 -> "EXPECTATION_FAILED",
    422 -> "UNPROCESSABLE_ENTITY",
    423 -> "LOCKED",
    424 -> "FAILED_DEPENDENCY",
    426 -> "UPGRADE_REQUIRED",
    428 -> "PRECONDITION_REQUIRED",
    429 -> "TOO_MANY_REQUESTS",
    500 -> "INTERNAL_SERVER_ERROR",
    501 -> "NOT_IMPLEMENTED",
    502 -> "BAD_GATEWAY",
    503 -> "SERVICE_UNAVAILABLE",
    504 -> "GATEWAY_TIMEOUT",
    505 -> "HTTP_VERSION_NOT_SUPPORTED",
    507 -> "INSUFFICIENT_STORAGE",
    511 -> "NETWORK_AUTHENTICATION_REQUIRED"
  )
}
