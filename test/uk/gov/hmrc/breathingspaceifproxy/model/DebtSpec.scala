/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json._
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class DebtSpec extends AnyFunSuite with BaseSpec {

  val chargeReference = "chargeReference"
  val chargeDescription = "chargeDescription"
  val chargeAmount: BigDecimal = BigDecimal(10)
  val chargeCreationDate: LocalDate = LocalDate.now()
  val chargeDueDate: LocalDate = LocalDate.now()

  test("the Debt objects should be deserialized correctly") {

    val utrAssociatedWithCharge = Some("utrAssociatedWithCharge")

    val debt = Debt(
      chargeReference,
      chargeDescription,
      chargeAmount,
      chargeCreationDate,
      chargeDueDate,
      utrAssociatedWithCharge
    )

    val expectedJson =
      """{
        |"chargeReference":"chargeReference",
        |"chargeDescription":"chargeDescription",
        |"chargeAmount":10,
        |"chargeCreationDate":"2022-04-19",
        |"chargeDueDate":"2022-04-19",
        |"utrAssociatedWithCharge":"utrAssociatedWithCharge"
        |}""".stripMargin

    Json.parse(expectedJson) shouldBe Json.toJson(debt)
  }

  test("the Debt objects should be deserialized correctly with utrAssociatedWithCharge as None") {

    val utrAssociatedWithCharge = None

    val debt = Debt(
      chargeReference,
      chargeDescription,
      chargeAmount,
      chargeCreationDate,
      chargeDueDate,
      utrAssociatedWithCharge
    )

    val expectedJson =
      """{
        |"chargeReference":"chargeReference",
        |"chargeDescription":"chargeDescription",
        |"chargeAmount":10,
        |"chargeCreationDate":"2022-04-19",
        |"chargeDueDate":"2022-04-19"
        |}""".stripMargin

    Json.parse(expectedJson) shouldBe Json.toJson(debt)
  }
}
