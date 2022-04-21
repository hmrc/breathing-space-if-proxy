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

package uk.gov.hmrc.breathingspaceifproxy.support

import org.scalacheck.Arbitrary
import uk.gov.hmrc.breathingspaceifproxy.model.Debt

trait Arbitraries {

  implicit val debtArbitrary: Arbitrary[Debt] = asArbitrary(Debt.apply)

  // Method takes a build function that takes the n arguments and produces a value
  // This is useful as it means we do not need to provide the arguments when we call
  // the method and allows the compiler to infer the types for us
  def asArbitrary[A, B, C, D, E, F, Out](build: (A, B, C, D, E, F) => Out)(
    implicit arb: Arbitrary[(A, B, C, D, E, F)]
  ): Arbitrary[Out] =
    Arbitrary(arb.arbitrary.map(build.tupled))
}
