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
import play.api.http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.breathingspaceifproxy.ResponseValidation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.{EisConnector, HeaderHandler}
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model.*
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UnderpaymentsConnector @Inject() (httpClientV2: HttpClientV2, metricRegistryParam: MetricRegistry)(implicit
  appConfig: AppConfig,
  val eisConnector: EisConnector,
  ec: ExecutionContext
) extends HttpAPIMonitor
    with HeaderHandler {

  import UnderpaymentsConnector._

  override val metricRegistry: MetricRegistry = metricRegistryParam

  def get(nino: Nino, periodId: UUID)(implicit requestId: RequestId): ResponseValidation[Underpayments] =
    eisConnector.monitor {
      monitor(s"ConsumedAPI-${requestId.endpointId}") {
        val updatedHc   = hc.withExtraHeaders(headers: _*)
        val fullUrl     = url(nino, periodId)
        val apiResponse = httpClientV2
          .get(url"$fullUrl")(updatedHc)
          .execute[Either[UpstreamErrorResponse, HttpResponse]](readEitherOf(readRaw), implicitly)

        apiResponse.map {
          case Right(response) =>
            response.status match {
              case OK         => response.json.as[Underpayments].validNec
              case NO_CONTENT => Underpayments(List()).validNec
              case _          =>
                ErrorItem(BaseError.INTERNAL_SERVER_ERROR, Some(s"Unexpected status: ${response.status}")).invalidNec
            }
          case Left(error)     =>
            ErrorItem(BaseError.INTERNAL_SERVER_ERROR, Some(error.message)).invalidNec
        }
      }
    }
}

object UnderpaymentsConnector {
  def url(nino: Nino, periodId: UUID)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkBaseUrl}${path(nino, periodId)}"

  def path(nino: Nino, periodId: UUID)(implicit appConfig: AppConfig): String =
    s"/${appConfig.integrationFrameworkContext}/breathing-space/${nino.value}/$periodId/coding-out-debts"
}
