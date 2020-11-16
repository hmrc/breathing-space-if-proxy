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

import ai.x.play.json.{BaseNameEncoder, Jsonx}
import cats.syntax.option._
import play.api.libs.json.{Json, OFormat}

trait Detail

abstract class DetailsData[T <: Detail] {
  val fields: String
  val format: OFormat[T]
}

// Details (full population) --------------------------------------------------------------

final case class NameData(
  nameSequenceNumber: Option[Int] = none,
  nameType: Option[Int] = none,
  titleType: Option[Int] = none,
  requestedName: Option[String] = none,
  nameStartDate: Option[LocalDate] = none,
  nameEndDate: Option[LocalDate] = none,
  otherTitle: Option[String] = none,
  honours: Option[String] = none,
  firstForename: Option[String] = none,
  secondForename: Option[String] = none,
  surname: Option[String] = none
)
object NameData {
  implicit val format = Json.format[NameData]
}

final case class NameList(name: List[NameData])
object NameList { implicit val format = Json.format[NameList] }

// --------------------------------------------------------------------------------

final case class AddressData(
  addressSequenceNumber: Option[Int] = none,
  addressSource: Option[Int] = none,
  countryCode: Option[Int] = none,
  addressType: Option[Int] = none,
  addressStatus: Option[Int] = none,
  addressStartDate: Option[LocalDate] = none,
  addressEndDate: Option[LocalDate] = none,
  addressLastConfirmedDate: Option[LocalDate] = none,
  vpaMail: Option[Int] = none,
  deliveryInfo: Option[String] = none,
  pafReference: Option[String] = none,
  addressLine1: Option[String] = none,
  addressLine2: Option[String] = none,
  addressLine3: Option[String] = none,
  addressLine4: Option[String] = none,
  addressLine5: Option[String] = none,
  addressPostcode: Option[String] = none
)
object AddressData { implicit val format = Json.format[AddressData] }

final case class AddressList(address: List[AddressData])
object AddressList { implicit val format = Json.format[AddressList] }

// --------------------------------------------------------------------------------

final case class Indicators(
  manualCodingInd: Option[Int] = none,
  manualCodingReason: Option[Int] = none,
  manualCodingOther: Option[String] = none,
  manualCorrInd: Option[Int] = none,
  manualCorrReason: Option[String] = none,
  additionalNotes: Option[String] = none,
  deceasedInd: Option[Int] = none,
  s128Ind: Option[Int] = none,
  noAllowInd: Option[Int] = none,
  eeaCmnwthInd: Option[Int] = none,
  noRepaymentInd: Option[Int] = none,
  saLinkInd: Option[Int] = none,
  noATSInd: Option[Int] = none,
  taxEqualBenInd: Option[Int] = none,
  p2ToAgentInd: Option[Int] = none,
  digitallyExcludedInd: Option[Int] = none,
  bankruptcyInd: Option[Int] = none,
  bankruptcyFiledDate: Option[LocalDate] = none,
  utr: Option[String] = none,
  audioOutputInd: Option[Int] = none,
  welshOutputInd: Option[Int] = none,
  largePrintOutputInd: Option[Int] = none,
  brailleOutputInd: Option[Int] = none,
  specialistBusinessArea: Option[Int] = none,
  saStartYear: Option[String] = none,
  saFinalYear: Option[String] = none,
  digitalP2Ind: Option[Int] = none
)
object Indicators {
  implicit val encoder = BaseNameEncoder()
  implicit val format = Jsonx.formatCaseClass[Indicators]
}

// --------------------------------------------------------------------------------

final case class ResidencyData(
  residencySequenceNumber: Option[Int] = none,
  dateLeavingUK: Option[LocalDate] = none,
  dateReturningUK: Option[LocalDate] = none,
  residencyStatusFlag: Option[Int] = none
)
object ResidencyData { implicit val format = Json.format[ResidencyData] }

final case class ResidencyList(residency: List[ResidencyData])
object ResidencyList { implicit val format = Json.format[ResidencyList] }

// --------------------------------------------------------------------------------

final case class IndividualDetails(
  nino: String,
  ninoSuffix: Option[String] = none,
  accountStatusType: Option[Int] = none,
  sex: Option[String] = none,
  dateOfEntry: Option[LocalDate] = none,
  dateOfBirth: Option[LocalDate] = none,
  dateOfBirthStatus: Option[Int] = none,
  dateOfDeath: Option[LocalDate] = none,
  dateOfDeathStatus: Option[Int] = none,
  dateOfRegistration: Option[LocalDate] = none,
  registrationType: Option[Int] = none,
  adultRegSerialNumber: Option[String] = none,
  cesaAgentIdentifier: Option[String] = none,
  cesaAgentClientReference: Option[String] = none,
  permanentTSuffixCaseIndicator: Option[Int] = none,
  currOptimisticLock: Option[Int] = none,
  liveCapacitorInd: Option[Int] = none,
  liveAgentInd: Option[Int] = none,
  ntTaxCodeInd: Option[Int] = none,
  mergeStatus: Option[Int] = none,
  marriageStatusType: Option[Int] = none,
  crnIndicator: Option[Int] = none,
  nameList: Option[NameList] = none,
  addressList: Option[AddressList] = none,
  residencyList: Option[ResidencyList] = none,
  indicators: Option[Indicators] = none
) extends Detail

object FullDetails extends DetailsData[IndividualDetails] {
  val fields = ""
  implicit val encoder = BaseNameEncoder()
  implicit val format: OFormat[IndividualDetails] = Jsonx.formatCaseClass[IndividualDetails]
}
