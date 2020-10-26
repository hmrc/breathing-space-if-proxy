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

import play.api.libs.json.{Json, OFormat}

trait Detail

abstract class DetailData[T <: Detail] {
  val fields: String
  val format: OFormat[T]
}

// Detail0 --------------------------------------------------------------------------------

final case class Detail0(nino: String, dateOfBirth: LocalDate, crnIndicator: Int) extends Detail
object DetailData0 extends DetailData[Detail0] {
  val fields = "?fields=details(nino,dateOfBirth,cnrIndicator)"
  val format = Json.format[Detail0]
}

// Detail1 --------------------------------------------------------------------------------

final case class NameDataForDetail1(
  firstForename: Option[String],
  surname: Option[String],
  secondForename: Option[String] = None
)
object NameDataForDetail1 { implicit val format: OFormat[NameDataForDetail1] = Json.format[NameDataForDetail1] }

final case class NameListForDetail1(name: List[NameDataForDetail1])
object NameListForDetail1 { implicit val format = Json.format[NameListForDetail1] }

final case class Detail1(nino: String, dateOfBirth: LocalDate, nameList: NameListForDetail1) extends Detail
object DetailData1 extends DetailData[Detail1] {
  val fields = "?fields=details(nino,dateOfBirth),namelist(name(firstForename,secondForename,surname))"
  val format = Json.format[Detail1]
}

// --------------------------------------------------------------------------------
