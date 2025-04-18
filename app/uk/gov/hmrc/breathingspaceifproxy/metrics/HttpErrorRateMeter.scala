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

package uk.gov.hmrc.breathingspaceifproxy.metrics

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import com.codahale.metrics.MetricRegistry
import play.api.Logging
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}

trait HttpErrorRateMeter extends Logging {

  val metricRegistry: MetricRegistry

  private def meterName[T](serviceName: String, statusCode: Int): String =
    if (statusCode >= 500) s"Http5xxErrorCount-$serviceName" else s"Http4xxErrorCount-$serviceName"

  def countErrors[T](serviceName: String)(future: Future[T])(implicit ec: ExecutionContext): Future[T] =
    future.andThen {
      case Success(response: HttpResponse) if response.status >= 400 => record(meterName(serviceName, response.status))
      case Failure(exception: UpstreamErrorResponse)                 => record(meterName(serviceName, exception.statusCode))
      case Failure(exception: HttpException)                         => record(meterName(serviceName, exception.responseCode))
      case Failure(_: Throwable)                                     => record(meterName(serviceName, 500))
    }

  private def record[T](name: String): Unit = {
    metricRegistry.getMeters.getOrDefault(name, metricRegistry.meter(name)).mark()
    logger.debug(s"metric-event::meter::$name::recorded")
  }
}
