/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.Inject
import akka.stream.Materializer
import play.api.http.{DefaultHttpFilters, EnabledFilters}
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class HttpFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      result.withHeaders("Cache-Control" -> "no-cache, no-store, must-revalidate")
    }
  }
}

class Filters @Inject()(
  defaultFilters: EnabledFilters,
  httpFilter: HttpFilter
) extends DefaultHttpFilters(defaultFilters.filters :+ httpFilter: _*)
