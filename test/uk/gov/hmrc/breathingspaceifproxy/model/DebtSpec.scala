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

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json._
import uk.gov.hmrc.breathingspaceifproxy.support.{Arbitraries, BaseSpec}

import scala.math.BigDecimal.RoundingMode

class DebtSpec extends AnyFunSuite with BaseSpec with ScalaCheckPropertyChecks with Arbitraries {

  test("Debt should serialize and deserialize only rounding chargeAmount") {

    forAll { (debt: Debt) =>
      val expectedResult = debt.copy(
        chargeAmount = debt.chargeAmount.setScale(2, RoundingMode.HALF_EVEN)
      )

      Json.toJson(debt).as[Debt] shouldBe expectedResult
    }
  }
}
