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

import cats.syntax.validated._
import com.codahale.metrics.MetricRegistry
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.breathingspaceifproxy.ResponseValidation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.{EisConnector, HeaderHandler}
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, HttpReads, HttpResponse}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UnderpaymentsConnector @Inject()(http: HttpClient, metricRegistryParam: MetricRegistry)(
  implicit appConfig: AppConfig,
  val eisConnector: EisConnector,
  ec: ExecutionContext
) extends HttpAPIMonitor
    with HeaderHandler {

  import UnderpaymentsConnector._

  override lazy val metricRegistry: MetricRegistry = metricRegistryParam

  def get(nino: Nino, periodId: UUID)(implicit requestId: RequestId): ResponseValidation[Underpayments] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        http
          .GET[Underpayments](Url(url(nino, periodId)).value, headers = headers)(
            UnderpaymentsHttpReads.reads,
            implicitly,
            implicitly
          )
          .map(_.validNec)
      }
    }

  private object UnderpaymentsHttpReads {
    def reads(implicit rds: HttpReads[Underpayments]): HttpReads[Underpayments] =
      (method: String, url: String, response: HttpResponse) =>
        response.status match {
          case NO_CONTENT => Underpayments(List())
          case _ => rds.read(method, url, response)
        }
  }
}

object UnderpaymentsConnector {
  def url(nino: Nino, periodId: UUID)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkBaseUrl}${path(nino, periodId)}"

  def path(nino: Nino, periodId: UUID)(implicit appConfig: AppConfig): String =
    s"/${appConfig.integrationFrameworkContext}/breathing-space/${nino.value}/$periodId/coding-out-debts"
}
