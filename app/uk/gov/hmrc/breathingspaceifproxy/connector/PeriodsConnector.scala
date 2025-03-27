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
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.breathingspaceifproxy.*
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.{EisConnector, HeaderHandler}
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model.*
import uk.gov.hmrc.breathingspaceifproxy.repository.{CacheRepository, Cacheable}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PeriodsConnector @Inject() (
  httpClientV2: HttpClientV2,
  metricRegistryParam: MetricRegistry,
  val cacheRepository: CacheRepository
)(implicit
  appConfig: AppConfig,
  val eisConnector: EisConnector,
  ec: ExecutionContext
) extends HttpAPIMonitor
    with HeaderHandler
    with Cacheable {

  import PeriodsConnector._

  override val metricRegistry: MetricRegistry = metricRegistryParam

  def get(nino: Nino)(implicit requestId: RequestId): ResponseValidation[PeriodsInResponse] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        val updatedHc = hc.withExtraHeaders(headers: _*)
        val fullUrl   = url(nino)
        httpClientV2
          .get(url"$fullUrl")(updatedHc)
          .execute[PeriodsInResponse]
          .map(_.validNec)
      }
    }

  def post(nino: Nino, postPeriods: PostPeriodsInRequest)(implicit
    requestId: RequestId
  ): ResponseValidation[PeriodsInResponse] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        clear(nino).flatMap { _ =>
          val updatedHc = hc.withExtraHeaders(headers: _*)
          val fullUrl   = url(nino)
          httpClientV2
            .post(url"$fullUrl")(updatedHc)
            .withBody(postPeriods)
            .execute[PeriodsInResponse]
            .map(_.validNec)
        }
      }
    }

  def put(nino: Nino, putPeriods: List[PutPeriodInRequest])(implicit
    requestId: RequestId
  ): ResponseValidation[PeriodsInResponse] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        clear(nino).flatMap { _ =>
          val updatedHc = hc.withExtraHeaders(headers: _*)
          val fullUrl   = url(nino)
          httpClientV2
            .put(url"$fullUrl")(updatedHc)
            .withBody(PutPeriodsInRequest(putPeriods))
            .execute[PeriodsInResponse]
            .map(_.validNec)
        }
      }
    }
}

object PeriodsConnector {

  def path(nino: Nino)(implicit appConfig: AppConfig): String =
    s"/${appConfig.integrationFrameworkContext}/breathing-space/NINO/${nino.value}/periods"

  def url(nino: Nino)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkBaseUrl}${path(nino)}"
}
