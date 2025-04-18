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

package uk.gov.hmrc.breathingspaceifproxy.config

import org.apache.pekko.stream.Materializer
import play.api.http.{DefaultHttpFilters, EnabledFilters}
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HttpFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext, appConfig: AppConfig) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] =
    nextFilter(requestHeader).map { result =>
      result.withHeaders("Cache-Control" -> appConfig.httpHeaderCacheControl)
    }
}

class Filters @Inject() (
  defaultFilters: EnabledFilters,
  httpFilter: HttpFilter
) extends DefaultHttpFilters(defaultFilters.filters :+ httpFilter: _*)
