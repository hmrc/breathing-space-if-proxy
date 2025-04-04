/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class ErrorItemSpec extends AnyFunSuite with BaseSpec with MockitoSugar {

  val entryName            = "EntryName"
  val message              = "Message:"
  val baseError: BaseError = mock[BaseError]
  when(baseError.entryName).thenReturn(entryName)
  when(baseError.message).thenReturn(message)

  test("the ErrorItem objects should be deserialized correctly with details") {

    val details   = Some("details")
    val errorItem = ErrorItem(baseError, details)

    val expectedJson =
      s"""
        |{
        | "code":"$entryName",
        | "message":"$message${details.get}"
        |}
        |""".stripMargin

    Json.parse(expectedJson) shouldBe Json.toJson(errorItem)
  }

  test("the ErrorItem objects should be deserialized correctly without details") {

    val details   = None
    val errorItem = ErrorItem(baseError, details)

    val expectedJson =
      s"""
         |{
         | "code":"$entryName",
         | "message":"$message"
         |}
         |""".stripMargin

    Json.parse(expectedJson) shouldBe Json.toJson(errorItem)
  }
}
