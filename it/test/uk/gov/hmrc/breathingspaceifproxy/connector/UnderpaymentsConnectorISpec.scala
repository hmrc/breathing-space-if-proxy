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
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.{INTERNAL_SERVER_ERROR, _}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Underpayments_GET
import uk.gov.hmrc.breathingspaceifproxy.model.{RequestId, Underpayment, Underpayments}
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

import java.util.UUID

class UnderpaymentsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  private val connector = inject[UnderpaymentsConnector]
  implicit val requestId: RequestId = genRequestId(BS_Underpayments_GET, connector.eisConnector)

  "get" should {
    "return a UnderpaymentsResponse instance when it receives a 200(OK) response" in {
      val nino = genNino
      val periodId = UUID.randomUUID()
      val url = UnderpaymentsConnector.path(nino, periodId)
      val validUnderpaymentsResponse = Underpayments(List(Underpayment("2022", 100.01, "PAYE UP")))
      val responsePayload = Json.toJson(validUnderpaymentsResponse).toString
      stubCall(HttpMethod.Get, url, OK, responsePayload)

      val response = await(connector.get(nino, periodId))

      verifyHeaders(HttpMethod.Get, url)
      assert(response.fold(_ => false, underpayments => underpayments.underPayments.nonEmpty))
    }

    "return an empty UnderpaymentsResponse instance when it receives a 204(NO CONTENT) response" in {
      val nino = genNino
      val periodId = UUID.randomUUID()
      val url = UnderpaymentsConnector.path(nino, periodId)
      stubCall(HttpMethod.Get, url, NO_CONTENT, "")

      val response = await(connector.get(nino, periodId))

      verifyHeaders(HttpMethod.Get, url)
      assert(response.fold(_ => false, underpayments => underpayments.underPayments.isEmpty))
    }

    "return RESOURCE_NOT_FOUND when the provided resource is unknown" in {
      verifyGetResponse(NOT_FOUND, RESOURCE_NOT_FOUND, "RESOURCE_NOT_FOUND".some)
    }

    "return CONFLICTING_REQUEST in case of duplicated requests" in {
      verifyGetResponse(CONFLICT, CONFLICTING_REQUEST)
    }

    "return SERVER_ERROR for any 4xx error, 404 and 409 excluded" in {
      verifyGetResponse(BAD_REQUEST, INTERNAL_SERVER_ERROR)
    }

    "return SERVER_ERROR for any 5xx error, 502, 503 and 504 excluded" in {
      verifyGetResponse(NOT_IMPLEMENTED, SERVER_ERROR)
    }
  }

  private def verifyGetResponse(status: Int, baseError: BaseError, code: Option[String] = None): Assertion = {
    val nino = genNino
    val periodId = UUID.randomUUID()
    val url = UnderpaymentsConnector.path(nino, periodId)
    stubCall(HttpMethod.Get, url, status, errorResponseFromIF(code.fold(baseError.entryName)(identity)))

    val response = await(connector.get(nino, periodId))

    verifyHeaders(HttpMethod.Get, url)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
