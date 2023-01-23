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
import play.api.libs.json._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.{EisConnector, HeaderHandler}
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.repository.{CacheRepository, Cacheable}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class PeriodsConnector @Inject()(http: HttpClient, metrics: Metrics, val cacheRepository: CacheRepository)(
  implicit appConfig: AppConfig,
  val eisConnector: EisConnector,
  ec: ExecutionContext
) extends HttpAPIMonitor
    with HeaderHandler
    with Cacheable {

  import PeriodsConnector._

  override lazy val metricRegistry: MetricRegistry = metrics.defaultRegistry

  def get(nino: Nino)(implicit requestId: RequestId): ResponseValidation[PeriodsInResponse] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        http.GET[PeriodsInResponse](Url(url(nino)).value, headers = headers).map(_.validNec)
      }
    }

  def post(nino: Nino, postPeriods: PostPeriodsInRequest)(
    implicit requestId: RequestId
  ): ResponseValidation[PeriodsInResponse] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        clear(nino).flatMap { _ =>
          http
            .POST[JsValue, PeriodsInResponse](Url(url(nino)).value, Json.toJson(postPeriods), headers)
            .map(_.validNec)
        }
      }
    }

  def put(nino: Nino, putPeriods: List[PutPeriodInRequest])(
    implicit requestId: RequestId
  ): ResponseValidation[PeriodsInResponse] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        clear(nino).flatMap { _ =>
          http
            .PUT[JsValue, PeriodsInResponse](
              Url(url(nino)).value,
              Json.toJson(PutPeriodsInRequest(putPeriods)),
              headers
            )
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
