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
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class PeriodsConnector @Inject()(http: HttpClient, metrics: Metrics)(
  implicit appConfig: AppConfig,
  ec: ExecutionContext
) extends ConnectorHelper
    with HttpAPIMonitor {

  import PeriodsConnector._

  override lazy val metricRegistry: MetricRegistry = metrics.defaultRegistry

  def get(nino: Nino)(implicit headerValues: RequiredHeaderSet): Future[Result] = {
    implicit val urlWrapper = Url(url(nino))
    monitor("ConsumedAPI-Breathing-Space-Periods-GET") {
      http
        .GET[HttpResponse](urlWrapper.value)
        .flatMap(composeResponseFromIF)
        .recoverWith(logException)
    }
  }

  def post(vcpr: ValidatedCreatePeriodsRequest)(implicit headerValues: RequiredHeaderSet): Future[Result] = {
    implicit val urlWrapper = Url(url(vcpr.nino))

    monitor("ConsumedAPI-Breathing-Space-Periods-POST") {
      http
        .POST[JsValue, HttpResponse](urlWrapper.value, Json.obj("periods" -> vcpr.periods))
        .flatMap(composeResponseFromIF)
        .recoverWith(logException)
    }
  }
}

object PeriodsConnector {
  def url(nino: Nino)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkUrl}/api/v1/${nino.value}/periods"
}
