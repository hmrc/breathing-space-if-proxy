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

package uk.gov.hmrc.breathingspaceifproxy.connector.service

import scala.concurrent.{ExecutionContext, Future}

import cats.syntax.validated._
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.breathingspaceifproxy.ResponseValidation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorItem, RequestId}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpException}
import uk.gov.hmrc.http.UpstreamErrorResponse.{Upstream4xxResponse, Upstream5xxResponse}

trait UpstreamConnector extends HttpErrorFunctions with Logging with UsingCircuitBreaker {

  object Is5xx {
    def unapply(throwable: Throwable): Option[Int] =
      throwable match {
        case exc: HttpException if is5xx(exc.responseCode) => Some(exc.responseCode)
        case Upstream5xxResponse(error) => Some(error.statusCode)
        case _ => None
      }
  }

  override def breakOnException(throwable: Throwable): Boolean =
    throwable match {
      case Is5xx(_) => true
      case _ => false
    }

  val appConfig: AppConfig

  override protected def circuitBreakerConfig = CircuitBreakerConfig(
    appConfig.appName,
    appConfig.numberOfCallsToTriggerStateChange,
    appConfig.unavailablePeriodDuration,
    appConfig.unstablePeriodDuration
  )

  def currentState: String = circuitBreaker.currentState.name

  def monitor[T](f: => ResponseValidation[T])(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier,
    requestId: RequestId
  ): ResponseValidation[T] =
    withCircuitBreaker(f).recoverWith(handleUpstreamError)

  protected def handleUpstreamError[T](
    implicit requestId: RequestId
  ): PartialFunction[Throwable, ResponseValidation[T]] = {
    case exc: HttpException if is4xx(exc.responseCode) => handleUpstream4xxError(exc.responseCode, exc.message)
    case exc: HttpException if is5xx(exc.responseCode) => handleUpstream5xxError(exc.responseCode, exc.message)

    case Upstream4xxResponse(res) => handleUpstream4xxError(res.statusCode, res.message)
    case Upstream5xxResponse(res) => handleUpstream5xxError(res.statusCode, res.message)

    case throwable: Throwable =>
      val name = throwable.getClass.getSimpleName
      logger.error(s"Exception($name) caught for upstream request $requestId. ${throwable.getMessage}")
      Future.successful(ErrorItem(BaseError.INTERNAL_SERVER_ERROR).invalidNec)
  }

  private def handleUpstream4xxError[T](statusCode: Int, message: String)(
    implicit r: RequestId
  ): ResponseValidation[T] =
    statusCode match {
      case NOT_FOUND => notFound(message)
      case FORBIDDEN => logAndGenDownstreamResponse(warning, FORBIDDEN, message, BaseError.BREATHING_SPACE_EXPIRED)
      case CONFLICT => logAndGenDownstreamResponse(info, CONFLICT, message, BaseError.CONFLICTING_REQUEST)
      case TOO_MANY_REQUESTS => logAndGenDownstreamResponse(warning, statusCode, message, BaseError.TOO_MANY_REQUESTS)
      case _ => logAndGenDownstreamResponse(warning, statusCode, message, BaseError.INTERNAL_SERVER_ERROR)
    }

  private def handleUpstream5xxError[T](statusCode: Int, message: String)(
    implicit r: RequestId
  ): ResponseValidation[T] = logAndGenDownstreamResponse(error, statusCode, message, BaseError.SERVER_ERROR)

  val noDataFound = """"code":"NO_DATA_FOUND""""
  val notInBS = """"code":"IDENTIFIER_NOT_IN_BREATHINGSPACE""""
  val noPeriodIdFound = """"code":"BREATHINGSPACE_ID_NOT_FOUND""""
  val noResourceFound = """"code":"RESOURCE_NOT_FOUND""""
  val notIdentifierFound = """"code":"IDENTIFIER_NOT_FOUND""""

  private def notFound[T](response: String)(implicit requestId: RequestId): ResponseValidation[T] = {
    val message = response.replaceAll("\\s", "")

    val baseError =
      if (message.contains(noDataFound)) BaseError.NO_DATA_FOUND
      else if (message.contains(notInBS)) BaseError.NOT_IN_BREATHING_SPACE
      else if (message.contains(noPeriodIdFound)) BaseError.PERIOD_ID_NOT_FOUND
      else if (message.contains(noResourceFound)) BaseError.RESOURCE_NOT_FOUND
      else if (message.contains(notIdentifierFound)) BaseError.RESOURCE_NOT_FOUND
      else BaseError.INTERNAL_SERVER_ERROR

    logAndGenDownstreamResponse(error, NOT_FOUND, response, baseError)
  }

  private def logAndGenDownstreamResponse[T](
    log: String => Unit,
    statusCode: Int,
    message: String,
    baseError: BaseError
  )(
    implicit requestId: RequestId
  ): ResponseValidation[T] = {
    log(s"Error($statusCode) for $requestId. $message")
    Future.successful(ErrorItem(baseError).invalidNec)
  }

  private val error = (message: String) => logger.error(message)
  private val info = (message: String) => logger.info(message)
  private val warning = (message: String) => logger.warn(message)
}
