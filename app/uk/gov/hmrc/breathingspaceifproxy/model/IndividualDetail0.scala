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

// Details (Breathing Space Population) -------------------------------------------

final case class Details0(
  nino: String,
  dateOfBirth: Option[LocalDate] = none
)
object Details0 { implicit val format = Json.format[Details0] }

// --------------------------------------------------------------------------------

final case class NameData0(
  firstForename: Option[String] = none,
  secondForename: Option[String] = none,
  surname: Option[String] = none
)
object NameData0 { implicit val format = Json.format[NameData0] }

final case class NameList0(name: List[NameData0])
object NameList0 { implicit val format = Json.format[NameList0] }

// --------------------------------------------------------------------------------

final case class AddressData0(
  addressLine1: Option[String] = none,
  addressLine2: Option[String] = none,
  addressLine3: Option[String] = none,
  addressLine4: Option[String] = none,
  addressLine5: Option[String] = none,
  addressPostcode: Option[String] = none,
  countryCode: Option[Int] = none
)
object AddressData0 { implicit val format = Json.format[AddressData0] }

final case class AddressList0(address: List[AddressData0])
object AddressList0 { implicit val format = Json.format[AddressList0] }

// --------------------------------------------------------------------------------

final case class IndividualDetail0(
  details: Details0,
  nameList: Option[NameList0] = none,
  addressList: Option[AddressList0] = none
) extends Detail

object IndividualDetail0 extends DetailsData[IndividualDetail0] {

  val Details = "details(nino,dateOfBirth)"
  val NameList = "nameList(name(firstForename,secondForename,surname))"
  val AddressList =
    "addressList(address(addressLine1,addressLine2,addressLine3,addressLine4,addressLine5,addressPostcode,countryCode))"

  val fields = s"?fields=$Details,$NameList,$AddressList"

  implicit val format = Json.format[IndividualDetail0]
}
