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

package uk.gov.hmrc.breathingspaceifproxy.support

import uk.gov.hmrc.breathingspaceifproxy.model.Nino

trait TestData {

  val maybeNino = "MZ006526C"
  val nino = Nino(maybeNino)
  val unknownNino = Nino("MZ006526C")

  def debtorDetails(nino: Nino): String =
    s"""
       |{"nino" : "${nino.value}",
       | "firstName" : "John",
       | "lastName" : "Smith",
       | "dateOfBirth" : "1990-01-01",
       |}
     """.stripMargin
}
