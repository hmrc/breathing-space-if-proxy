/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.syntax.validated.*
import com.codahale.metrics.MetricRegistry
import uk.gov.hmrc.breathingspaceifproxy.*
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.{EisConnector, HeaderHandler}
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model.*
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class IndividualDetailsConnector @Inject() (httpClientV2: HttpClientV2, metricRegistryParam: MetricRegistry)(implicit
  appConfig: AppConfig,
  val eisConnector: EisConnector,
  ec: ExecutionContext
) extends HttpAPIMonitor
    with HeaderHandler {

  import IndividualDetailsConnector._

  override val metricRegistry: MetricRegistry = metricRegistryParam

  // Breathing Space Population
  def getDetails(nino: Nino)(implicit requestId: RequestId): ResponseValidation[IndividualDetails] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        val updatedHc = hc.withExtraHeaders(headers: _*)
        val fullUrl   = url(nino, IndividualDetails.fields)
        httpClientV2
          .get(url"$fullUrl")(updatedHc)
          .execute[IndividualDetails]
          .map(_.validNec)
      }
    }
}

object IndividualDetailsConnector {

  def path(nino: Nino, queryParams: String)(implicit appConfig: AppConfig): String =
    s"/${appConfig.integrationFrameworkContext}/details/NINO/${nino.value}$queryParams"

  def url(nino: Nino, queryParams: String)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkBaseUrl}${path(nino, queryParams)}"
}
