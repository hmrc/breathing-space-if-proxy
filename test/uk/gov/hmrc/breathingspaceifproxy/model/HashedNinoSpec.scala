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

import org.mockito.scalatest.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class HashedNinoSpec extends AnyFunSuite with BaseSpec with MockitoSugar {

  test("the generateHash method should return the expected hash for a given nino") {
    val nino = Nino("AS000001A")
    implicit val appConfig: AppConfig = mock[AppConfig]
    when(appConfig.ninoHashingKey).thenReturn("12345")

    val expectedHashedNino = "kbcJXlu0+8mY9fmwrT35aziF9vUTkW1HWQotqbI/Q0L93PEOBgol8CeCjtU1G/3X+lKFVXHLtS5kroUFLSWZTA=="

    val hashedNino = HashedNino(nino).generateHash()

    hashedNino shouldBe expectedHashedNino
  }

}
