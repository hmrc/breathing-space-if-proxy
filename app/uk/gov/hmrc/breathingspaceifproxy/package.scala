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

package uk.gov.hmrc

import uk.gov.hmrc.breathingspaceifproxy.model.Attended
import uk.gov.hmrc.http.HeaderCarrier

package object breathingspaceifproxy {

  lazy val HeaderClientId = "X-Client-Id"
  lazy val HeaderContext = "X-Context"
  lazy val HeaderCorrelationId = "X-Correlation-Id"

  lazy val MissingRequiredHeaders = s"Missing required headers($HeaderCorrelationId and/or $HeaderContext)"

  def invalidContextHeader(context: String): String =
    s"Invalid $HeaderContext Header($context). Valid values are: ${Attended.values.mkString(", ")}"

  def invalidNino(nino: String): String = s"Invalid Nino($nino)"

  def retrieveCorrelationId(implicit hc: HeaderCarrier): Option[String] =
    hc.extraHeaders.find(t => t._1.equals(HeaderCorrelationId)).map(_._2)
}
