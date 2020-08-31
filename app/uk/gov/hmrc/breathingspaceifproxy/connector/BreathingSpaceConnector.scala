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

package uk.gov.hmrc.breathingspaceifproxy.connector

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorResponse, Nino}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._

class BreathingSpaceConnector @Inject()(appConfig: AppConfig, http: HttpClient)
    extends HttpErrorFunctions
    with Logging {

  def retrieveIdentityDetails(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    implicit val url = BreathingSpaceConnector.retrieveIdentityDetailsUrl(appConfig, nino)

    http
      .GET[HttpResponse](url)
      .map(proxyHttpResponse)
      .recoverWith(logException)
  }

  private def logException(implicit url: String, hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case exc: HttpException =>
      val correlationId = retrieveCorrelationId
      logger.error(s"ERROR(${exc.responseCode}) for call($HeaderCorrelationId: $correlationId). ${exc.message}")
      ErrorResponse(exc.responseCode, exc.message, correlationId).value

    case throwable: Throwable =>
      logger.error(s"EXCEPTION for call($HeaderCorrelationId: $retrieveCorrelationId) to $url.", throwable)
      Future.failed(throwable)
  }

  private def logResponse(response: HttpResponse)(implicit url: String, hc: HeaderCarrier): Unit =
    response.status match {
      case status if is2xx(status) =>
        logger.debug(s"Status($status) for call($HeaderCorrelationId: $retrieveCorrelationId) to $url")

      case status => // 1xx, 3xx, 4xx, 5xx
        logger.error(s"ERROR($status) for call($HeaderCorrelationId: $retrieveCorrelationId) to $url")
        logger.debug(s"... with Body: ${response.body}")
    }

  private def proxyHttpResponse(response: HttpResponse)(implicit url: String, hc: HeaderCarrier): Result = {
    logResponse(response)
    val result = Status(response.status)(response.body)
      .withHeaders(response.headers.toList.map(header => (header._1, header._2.mkString(""))): _*)

    response.header(CONTENT_TYPE).fold(result)(result.as(_))
  }
}

object BreathingSpaceConnector {

  private val retrieveIdentityDetailsPartial = "/debtor/"

  def retrieveIdentityDetailsPath(appConfig: AppConfig, nino: Nino): String =
    s"/${appConfig.integrationFrameworkContext}$retrieveIdentityDetailsPartial${nino.value}"

  def retrieveIdentityDetailsUrl(appConfig: AppConfig, nino: Nino): String =
    s"${appConfig.integrationFrameworkUrl}$retrieveIdentityDetailsPartial${nino.value}"
}
