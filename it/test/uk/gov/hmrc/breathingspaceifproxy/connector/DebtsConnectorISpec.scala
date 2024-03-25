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

package uk.gov.hmrc.breathingspaceifproxy.connector

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status._
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Debts_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class DebtsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  val connector = inject[DebtsConnector]
  implicit val requestId = genRequestId(BS_Debts_GET, connector.etmpConnector)

  "get" should {
    "return a Debts instance when it receives a 200(OK) response" in {
      val nino = genNino
      val url = DebtsConnector.path(nino, periodId)
      stubCall(HttpMethod.Get, url, OK, debtsAsSentFromEis)

      val response = await(connector.get(nino, periodId))

      verifyHeaders(HttpMethod.Get, url)
      assert(response.fold(_ => false, _ => true))
    }

    "return BREATHING_SPACE_EXPIRED when Breathing Space has expired for the given Nino" in {
      verifyGetResponse(FORBIDDEN, BREATHING_SPACE_EXPIRED, "BREATHINGSPACE_EXPIRED".some)
    }

    "return NO_DATA_FOUND when the given Nino has no debts" in {
      verifyGetResponse(NOT_FOUND, NO_DATA_FOUND, "NO_DATA_FOUND".some)
    }

    "return NOT_IN_BREATHING_SPACE when the given Nino is not in Breathing Space" in {
      verifyGetResponse(NOT_FOUND, NOT_IN_BREATHING_SPACE, "IDENTIFIER_NOT_IN_BREATHINGSPACE".some)
    }

    "return RESOURCE_NOT_FOUND when the given Nino is unknown" in {
      verifyGetResponse(NOT_FOUND, RESOURCE_NOT_FOUND, "IDENTIFIER_NOT_FOUND".some)
    }

    "return PERIOD_ID_NOT_FOUND when the given periodId is unknown" in {
      verifyGetResponse(NOT_FOUND, PERIOD_ID_NOT_FOUND, "BREATHINGSPACE_ID_NOT_FOUND".some)
    }

    "return CONFLICTING_REQUEST in case of duplicated requests" in {
      verifyGetResponse(CONFLICT, CONFLICTING_REQUEST, "CONFLICTING_REQUEST".some)
    }

    "return SERVER_ERROR for any 4xx error, 403, 404 and 409 excluded" in {
      verifyGetResponse(BAD_REQUEST, BaseError.INTERNAL_SERVER_ERROR)
    }

    "return SERVER_ERROR for any 5xx error, 502, 503 and 504 excluded" in {
      verifyGetResponse(NOT_IMPLEMENTED, BaseError.SERVER_ERROR)
    }
  }

  private def verifyGetResponse(status: Int, baseError: BaseError, code: Option[String] = none): Assertion = {
    val nino = genNino
    val url = DebtsConnector.path(nino, periodId)
    stubCall(HttpMethod.Get, url, status, errorResponseFromIF(code.fold(baseError.entryName)(identity)))

    val response = await(connector.get(nino, periodId))

    verifyHeaders(HttpMethod.Get, url)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
