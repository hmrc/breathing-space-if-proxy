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

import cats.implicits._
import play.api.http.Status
import play.api.mvc._
import play.api.http.Status.CREATED
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.HeaderCarrierConverter

@Singleton
class PeriodsConnector @Inject()(http: HttpClient, metrics: Metrics)(
  implicit appConfig: AppConfig,
  ec: ExecutionContext
) extends ConnectorHelper
    with HttpAPIMonitor {

  import PeriodsConnector._

  override lazy val metricRegistry: MetricRegistry = metrics.defaultRegistry

  def get(nino: Nino)(implicit hc: HeaderCarrier): Future[Result] = {
    implicit val urlWrapper = Url(url(nino))
    monitor("ConsumedAPI-Breathing-Space-Periods-GET") {
      http
        .GET[HttpResponse](urlWrapper.value)
        .map(composeResponseFromIF)
        .recoverWith(logException)
    }
  }

  // TODO: only pass the three header values I need/expect, not everyone sent in the request
  def post(vcpr: ValidatedCreatePeriodsRequest)(implicit headerValues: RequiredHeaderSet): Future[Result] = {
    implicit val urlWrapper = Url(url(vcpr.nino))

    monitor("ConsumedAPI-Breathing-Space-Periods-POST") {
      http
        .POST[RequestedPeriods, HttpResponse](urlWrapper.value, vcpr.periods)
        .map(composeResponseFromIF)
        .recoverWith(logException)
    }
  }
}

object PeriodsConnector {
  def url(nino: Nino)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkUrl}/breathing-space-periods/api/v1/${nino.value}/periods"

  def parseIFPostBreathingSpaceResponse(
    response: HttpResponse
  )(implicit url: Url, reads: Reads[IFCreatePeriodsResponse]): Either[ErrorResponse, IFCreatePeriodsResponse] = {
    val maybeCorrelationId = response.header(Header.CorrelationId)
    response.status match {
      case CREATED =>
        validateResponseBody[IFCreatePeriodsResponse](response).left
          .map(
            throwable =>
              ErrorResponse(maybeCorrelationId, s"Exception caught while calling ${url.value}.", throwable.some)
          )

      case _ =>
        Left(ErrorResponse(maybeCorrelationId, s"Exception caught while calling ${url.value}."))
    }
  }

  def validateResponseBody[A](response: HttpResponse)(implicit reads: Reads[A]): Either[Throwable, A] = {
    val json = Json.parse(response.body)
    Either.catchNonFatal(json.as[A])
  }

  implicit def hc(implicit headerSet: RequiredHeaderSet): HeaderCarrier = {
    val ifRequestHeaders = Seq(
      (Header.CorrelationId, headerSet.correlationId.value),
      (Header.RequestType, headerSet.attended.toString),
      (Header.StaffId, headerSet.staffId.value)
    )

    // TODO: need to add something like this also:-, authorization = Some(Authorization(bearerToken)))
    HeaderCarrier(extraHeaders = ifRequestHeaders)
  }
}
