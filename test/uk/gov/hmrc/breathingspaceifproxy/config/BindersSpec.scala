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

package uk.gov.hmrc.breathingspaceifproxy.config

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorItem, Nino}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.INVALID_NINO
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class BindersSpec extends AnyFunSuite with BaseSpec {

  test("bind method in the Nino Binder should return a Nino object for a valid nino") {
    val key = "1234"
    val validNino = "AS000001A"
    val expectedNino = Right(Nino(validNino))
    Binders.ninoBinder.bind(key, validNino) shouldBe expectedNino
  }

  test("bind method in the Nino Binder should return an error list with the INVALID NINO error for an invalid nino") {
    val key = "12345"
    val invalidNino = "12345"
    val expectedError = Left(
      Json.stringify(
        Json.obj("errors" -> List(Json.toJson(ErrorItem(INVALID_NINO))))
      )
    )
    Binders.ninoBinder.bind(key, invalidNino) shouldBe expectedError
  }

  test("unbind method in the Nino Binder should return the Nino value") {
    val key = "12345"
    val ninoValue = "AS000001A"
    val nino = Nino(ninoValue)
    Binders.ninoBinder.unbind(key, nino) shouldBe ninoValue
  }
}
