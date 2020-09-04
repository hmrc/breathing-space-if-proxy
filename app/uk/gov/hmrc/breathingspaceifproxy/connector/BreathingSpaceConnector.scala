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

import scala.concurrent.{ExecutionContext, Future}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.{Status => HttpStatus}
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorResponse, Nino, Url}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class BreathingSpaceConnector @Inject()(appConfig: AppConfig, http: HttpClient, metrics: Metrics)
    extends BreathingSpaceConnectorHelper
    with HttpAPIMonitor {

  override lazy val metricRegistry: MetricRegistry = metrics.defaultRegistry

  def retrieveIdentityDetails(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    implicit val url = Url(retrieveIdentityDetailsUrl(appConfig, nino))

    monitor("ConsumedAPI-Debtor-Personal-Details-GET") {
      http
        .GET[HttpResponse](url.value)
        .map(composeResponseFromIF)
        .recoverWith(logException)
    }
  }
}

trait BreathingSpaceConnectorHelper extends HttpErrorFunctions with Logging {

  def composeResponseFromIF(response: HttpResponse)(implicit url: Url, hc: HeaderCarrier): Result = {
    logResponse(response)
    val result = Status(response.status)(response.body)
      .withHeaders(response.headers.toList.map(header => (header._1, header._2.mkString(""))): _*)

    response
      .header(CONTENT_TYPE)
      .fold {
        result.body.contentType.fold(result)(contentType => result.withHeaders(CONTENT_TYPE -> contentType))
      } {
        result.as(_)
      }
  }

  def logException(implicit url: Url, hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case httpException: HttpException =>
      val reasonToLog = s"HTTP error(${httpException.responseCode}) while calling ${url.value}."
      ErrorResponse(httpException.responseCode, reasonToLog, httpException, retrieveCorrelationId).value

    case throwable: Throwable =>
      val reasonToLog = s"Exception caught while calling ${url.value}."
      ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, reasonToLog, throwable, retrieveCorrelationId).value
  }

  def logResponse(response: HttpResponse)(implicit url: Url, hc: HeaderCarrier): Unit =
    response.status match {
      case status if is2xx(status) =>
        logger.debug(s"Status($status) ${calling(retrieveCorrelationId)} ${url.value}")

      case status => // 1xx, 3xx, 4xx, 5xx
        logger.error(s"ERROR($status) ${calling(retrieveCorrelationId)} ${url.value}")
        logger.debug(s"... with Body: ${response.body}")
    }

  private def calling(correlationId: Option[String]): String =
    correlationId.fold("while calling")(value => s"for call($HeaderCorrelationId: $value) to")

  private val retrieveIdentityDetailsPartial = "/debtor/"

  def retrieveIdentityDetailsPath(appConfig: AppConfig, nino: Nino): String =
    s"/${appConfig.integrationFrameworkContext}$retrieveIdentityDetailsPartial${nino.value}"

  def retrieveIdentityDetailsUrl(appConfig: AppConfig, nino: Nino): String =
    s"${appConfig.integrationFrameworkUrl}$retrieveIdentityDetailsPartial${nino.value}"
}
