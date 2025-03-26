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
import uk.gov.hmrc.breathingspaceifproxy.ResponseValidation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.{HeaderHandler, MemConnector}
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorItem, MemorandumInResponse, Nino, RequestId}
import uk.gov.hmrc.breathingspaceifproxy.repository.{CacheRepository, Cacheable}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MemorandumConnector @Inject() (
  httpClientV2: HttpClientV2,
  metricRegistryParam: MetricRegistry,
  val cacheRepository: CacheRepository
)(implicit
  appConfig: AppConfig,
  val memorandumConnector: MemConnector,
  hc: HeaderCarrier,
  ec: ExecutionContext
) extends HttpAPIMonitor
    with HeaderHandler
    with Cacheable {

  import MemorandumConnector._

  override val metricRegistry: MetricRegistry = metricRegistryParam

  def get(nino: Nino)(implicit requestId: RequestId): ResponseValidation[MemorandumInResponse] =
    memorandumConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        cache("memorandum")(nino) {
          val updatedHc   = hc.withExtraHeaders(headers: _*)
          val fullUrl     = url(nino)
          val apiResponse = httpClientV2
            .get(url"$fullUrl")(updatedHc)
            .execute[Either[UpstreamErrorResponse, HttpResponse]](readEitherOf(readRaw), implicitly)

          apiResponse.map {
            case Right(response) => response.json.as[MemorandumInResponse].validNec
            case Left(error)     => ErrorItem(BaseError.INTERNAL_SERVER_ERROR, Some(error.message)).invalidNec
          }
        }
      }
    }
}

object MemorandumConnector {
  def path(nino: Nino)(implicit appConfig: AppConfig): String =
    s"/${appConfig.integrationFrameworkContext}/breathing-space/${nino.value}/memorandum"

  def url(nino: Nino)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkBaseUrl}${path(nino)}"
}
