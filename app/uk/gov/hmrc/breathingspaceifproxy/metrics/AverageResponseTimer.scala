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
import scala.concurrent.duration._

import com.codahale.metrics.MetricRegistry
import play.api.Logging

trait AverageResponseTimer extends Logging {

  val metricRegistry: MetricRegistry

  def timer[T](serviceName: String)(function: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val start = System.nanoTime()
    function.andThen { case _ =>
      val duration  = Duration(System.nanoTime() - start, NANOSECONDS)
      val timerName = s"Timer-$serviceName"
      metricRegistry.getTimers
        .getOrDefault(
          timerName,
          metricRegistry.timer(timerName)
        )
        .update(duration.length, duration.unit)

      logger.debug(
        s"metric-event::timer::$timerName::duration:{'length':${duration.length}, 'unit':${duration.unit}}"
      )
    }
  }
}
