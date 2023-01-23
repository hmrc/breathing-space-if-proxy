/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.breathingspaceifproxy.ResponseValidation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.{HeaderHandler, MemConnector}
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model.{MemorandumInResponse, Nino, RequestId, Url}
import uk.gov.hmrc.breathingspaceifproxy.repository.{CacheRepository, Cacheable}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class MemorandumConnector @Inject()(http: HttpClient, metrics: Metrics, val cacheRepository: CacheRepository)(
  implicit appConfig: AppConfig,
  val memorandumConnector: MemConnector,
  ec: ExecutionContext
) extends HttpAPIMonitor
    with HeaderHandler
    with Cacheable {

  import MemorandumConnector._

  override lazy val metricRegistry: MetricRegistry = metrics.defaultRegistry

  def get(nino: Nino)(implicit requestId: RequestId): ResponseValidation[MemorandumInResponse] =
    memorandumConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        cache("memorandum")(nino) {
          http.GET[MemorandumInResponse](Url(url(nino)).value, headers = headers).map(_.validNec)
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
