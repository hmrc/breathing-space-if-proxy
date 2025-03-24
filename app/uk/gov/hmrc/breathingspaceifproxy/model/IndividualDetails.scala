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

package uk.gov.hmrc.breathingspaceifproxy.model

import cats.syntax.option.none
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

// Details (Breathing Space Population) -------------------------------------------

final case class Details(
  nino: String,
  dateOfBirth: Option[LocalDate] = none
)
object Details { implicit val format: OFormat[Details] = Json.format[Details] }

// --------------------------------------------------------------------------------

final case class NameData(
  firstForename: Option[String] = none,
  secondForename: Option[String] = none,
  surname: Option[String] = none,
  nameType: Option[Int] = none
)
object NameData { implicit val format: OFormat[NameData] = Json.format[NameData] }

final case class NameList(name: List[NameData])
object NameList { implicit val format: OFormat[NameList] = Json.format[NameList] }

// --------------------------------------------------------------------------------

final case class AddressData(
  addressLine1: Option[String] = none,
  addressLine2: Option[String] = none,
  addressLine3: Option[String] = none,
  addressLine4: Option[String] = none,
  addressLine5: Option[String] = none,
  addressPostcode: Option[String] = none,
  countryCode: Option[Int] = none,
  addressType: Option[Int] = none
)
object AddressData { implicit val format: OFormat[AddressData] = Json.format[AddressData] }

final case class AddressList(address: List[AddressData])
object AddressList { implicit val format: OFormat[AddressList] = Json.format[AddressList] }

// --------------------------------------------------------------------------------

final case class Indicators(
  welshOutputInd: Option[Int] = none
)
object Indicators { implicit val format: OFormat[Indicators] = Json.format[Indicators] }

// --------------------------------------------------------------------------------

final case class IndividualDetails(
  details: Details,
  nameList: Option[NameList] = none,
  addressList: Option[AddressList] = none,
  indicators: Option[Indicators] = none
)

object IndividualDetails {

  val Details     = "details(nino,dateOfBirth)"
  val NameList    = "nameList(name(firstForename,secondForename,surname,nameType))"
  val AddressList =
    "addressList(address(addressLine1,addressLine2,addressLine3,addressLine4,addressLine5,addressPostcode,countryCode,addressType))"
  val Indicators  = "indicators(welshOutputInd)"

  val fields = s"?fields=$Details,$NameList,$AddressList,$Indicators"

  implicit val format: OFormat[IndividualDetails] = Json.format[IndividualDetails]
}
