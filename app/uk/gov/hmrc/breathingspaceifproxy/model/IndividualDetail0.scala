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

import java.time.LocalDate

import cats.syntax.option.none
import play.api.libs.json.Json

final case class Detail0(
  nino: String,
  firstForename: Option[String] = none,
  secondForename: Option[String] = none,
  surname: Option[String] = none,
  dateOfBirth: Option[LocalDate] = none,
  addressLine1: Option[String] = none,
  addressLine2: Option[String] = none,
  addressLine3: Option[String] = none,
  addressLine4: Option[String] = none,
  addressLine5: Option[String] = none,
  addressPostcode: Option[String] = none,
  countryCode: Option[Int] = none
) extends Detail

object DetailData0 extends DetailsData[Detail0] {
  val fields =
    "?fields=details(nino,dateOfBirth),nameList(name(firstForename,secondForename,surname)),addressList(address(addressLine1,addressLine2,addressLine3,addressLine4,addressLine5,addressPostcode,countryCode))"

  val format = Json.format[Detail0]
}
