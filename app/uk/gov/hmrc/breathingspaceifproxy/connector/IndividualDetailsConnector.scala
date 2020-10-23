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

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

import cats.syntax.validated._
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class IndividualDetailsConnector @Inject()(http: HttpClient, metrics: Metrics)(
  implicit appConfig: AppConfig,
  ec: ExecutionContext
) extends ConnectorHelper
    with HttpAPIMonitor {

  import IndividualDetailsConnector._

  override lazy val metricRegistry: MetricRegistry = metrics.defaultRegistry

  def getMinimalPopulation(nino: Nino)(
    implicit requestId: RequestId,
    hc: HeaderCarrier
  ): ResponseValidation[MinimalPopulation] =
    monitor(s"ConsumedAPI-${requestId.endpointId}") {
      http
        .GET[MinimalPopulation](Url(url(nino, minimalPopulation)).value)
        .map(_.validNec)
        .recoverWith(handleUpstreamError)
    }
}

object IndividualDetailsConnector {

  lazy val minimalPopulation = "?fields=details(nino,dateOfBirth,cnrIndicator)"

  def path(nino: Nino, queryParams: String)(implicit appConfig: AppConfig): String =
    s"/${appConfig.integrationFrameworkContext}/details/NINO/${nino.value}${queryParams}"

  def url(nino: Nino, queryParams: String)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkBaseUrl}${path(nino, queryParams)}"
}
