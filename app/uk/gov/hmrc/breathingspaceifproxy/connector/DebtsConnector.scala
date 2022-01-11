/*
 * Copyright 2022 HM Revenue & Customs
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

import java.util.UUID
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

import cats.syntax.validated._
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.{EtmpConnector, HeaderHandler}
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class DebtsConnector @Inject()(http: HttpClient, metrics: Metrics)(
  implicit appConfig: AppConfig,
  val etmpConnector: EtmpConnector,
  ec: ExecutionContext
) extends HttpAPIMonitor
    with HeaderHandler {

  import DebtsConnector._

  override lazy val metricRegistry: MetricRegistry = metrics.defaultRegistry

  def get(nino: Nino, periodId: UUID)(implicit requestId: RequestId): ResponseValidation[Debts] =
    etmpConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        http.GET[List[Debt]](Url(url(nino, periodId)).value, headers = headers).map(Debts(_).validNec)
      }
    }
}

object DebtsConnector {

  def path(nino: Nino, periodId: UUID)(implicit appConfig: AppConfig): String =
    s"/${appConfig.integrationFrameworkContext}/breathing-space/NINO/${nino.value}/${periodId}/debts"

  def url(nino: Nino, periodId: UUID)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkBaseUrl}${path(nino, periodId)}"
}
