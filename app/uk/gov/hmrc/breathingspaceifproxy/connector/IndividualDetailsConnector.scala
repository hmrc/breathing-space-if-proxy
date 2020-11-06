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
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.metrics.HttpAPIMonitor
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.http._

@Singleton
class IndividualDetailsConnector @Inject()(http: HttpClient, metrics: Metrics)(
  implicit appConfig: AppConfig,
  ec: ExecutionContext
) extends ConnectorHelper
    with HttpAPIMonitor {

  import IndividualDetailsConnector._

  override lazy val metricRegistry: MetricRegistry = metrics.defaultRegistry

  def get[T <: Detail](nino: Nino, detailData: DetailsData[T])(
    implicit requestId: RequestId,
    hc: HeaderCarrier
  ): ResponseValidation[T] =
    monitor(s"ConsumedAPI-${requestId.endpointId}") {
      implicit val format = detailData.format
      http
        .GET[Validation[T]](Url(url(nino, detailData.fields)).value)
        .recoverWith(handleUpstreamError)
    }

  implicit def reads[T <: Detail](implicit requestId: RequestId, rds: Reads[T]): HttpReads[Validation[T]] =
    new HttpReads[Validation[T]] {
      override def read(method: String, url: String, response: HttpResponse): Validation[T] =
        response.status match {
          case Status.OK =>
            response.json.validate[T] match {
              case JsSuccess(value, _) => value.validNec
              case JsError(_) =>
                logErrorAndGenResponse(s"got an unexpected payload(${response.body})", response, SERVER_ERROR)
            }

          case Status.NOT_FOUND => ErrorItem(RESOURCE_NOT_FOUND).invalidNec
          case Status.CONFLICT => ErrorItem(CONFLICTING_REQUEST).invalidNec
          case status => logErrorAndGenResponse(s"got an unexpected status($status)", response, SERVER_ERROR)
        }
    }

  private def logErrorAndGenResponse[T](message: String, response: HttpResponse, baseError: BaseError)(
    implicit requestId: RequestId
  ): Validation[T] = {
    logger.error(s"$requestId $message. Body: ${response.body}")
    ErrorItem(baseError).invalidNec
  }
}

object IndividualDetailsConnector {

  def path(nino: Nino, queryParams: String)(implicit appConfig: AppConfig): String =
    s"/${appConfig.integrationFrameworkContext}/details/NINO/${nino.value}${queryParams}"

  def url(nino: Nino, queryParams: String)(implicit appConfig: AppConfig): String =
    s"${appConfig.integrationFrameworkBaseUrl}${path(nino, queryParams)}"
}
