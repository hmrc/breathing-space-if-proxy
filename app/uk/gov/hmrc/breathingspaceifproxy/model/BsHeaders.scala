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

package uk.gov.hmrc.breathingspaceifproxy.model

case class BsHeaders(correlationId: String, context: Attended, clientId: Option[String] = None)

trait Attended

object Attended {
  case object PEGA_ATTENDED extends Attended
  case object PEGA_UNATTENDED extends Attended

  def fromString(value: String): Option[Attended] =
    value match {
      case "PEGA_ATTENDED" => Some(PEGA_ATTENDED)
      case "PEGA_UNATTENDED" => Some(PEGA_UNATTENDED)
      case _ => None
    }
}
