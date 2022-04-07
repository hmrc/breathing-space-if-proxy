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

package uk.gov.hmrc.breathingspaceifproxy.connector

import org.scalatest.Assertion
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Details_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class IndividualDetailsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  val connector = inject[IndividualDetailsConnector]
  implicit val requestId = genRequestId(BS_Details_GET, connector.eisConnector)

  "get" should {
    "return an IndividualDetails instance when it receives the relative \"fields\" query parameter" in {
      val nino = genNino
      val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string
      val queryParams = detailQueryParams(IndividualDetails.fields)

      stubCall(HttpMethod.Get, path, OK, Json.toJson(details(nino)).toString, queryParams)

      val response = await(connector.getDetails(nino))

      verifyHeaders(HttpMethod.Get, path, queryParams)
      assert(response.fold(_ => false, _ => true))
    }

    "return RESOURCE_NOT_FOUND when the provided resource is unknown" in {
      verifyErrorResponse(genNino, NOT_FOUND, RESOURCE_NOT_FOUND)
    }

    "return CONFLICTING_REQUEST in case of duplicated requests" in {
      verifyErrorResponse(genNino, CONFLICT, CONFLICTING_REQUEST)
    }

    "return SERVER_ERROR if the returned payload is unexpected" in {
      val nino = genNino
      val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string

      val unexpectedPayload = Json.parse("""{"dateOfRegistration":"2020-01-01","sex":"M"}""").toString
      val queryParams = detailQueryParams(IndividualDetails.fields)

      stubCall(HttpMethod.Get, path, OK, unexpectedPayload, queryParams)

      val response = await(connector.getDetails(nino))

      verifyHeaders(HttpMethod.Get, path, queryParams)
      response.fold(_.head.baseError shouldBe INTERNAL_SERVER_ERROR, _ => notAnErrorInstance)
    }

    "return SERVER_ERROR for any 4xx error, 404 and 409 excluded" in {
      verifyErrorResponse(genNino, BAD_REQUEST, INTERNAL_SERVER_ERROR)
    }

    "return UPSTREAM_BAD_GATEWAY for a 502(BAD_GATEWAY) error" in {
      verifyErrorResponse(genNino, BAD_GATEWAY, UPSTREAM_BAD_GATEWAY)
    }

    "return UPSTREAM_SERVICE_UNAVAILABLE for a 503(SERVICE_UNAVAILABLE) error" in {
      verifyErrorResponse(genNino, SERVICE_UNAVAILABLE, UPSTREAM_SERVICE_UNAVAILABLE)
    }

    "return UPSTREAM_TIMEOUT for a 504(GATEWAY_TIMEOUT) error" in {
      verifyErrorResponse(genNino, GATEWAY_TIMEOUT, UPSTREAM_TIMEOUT)
    }

    "return SERVER_ERROR for any 5xx error (excluding 502,503,504)" in {
      verifyErrorResponse(genNino, NOT_IMPLEMENTED, UPSTREAM_SERVICE_UNAVAILABLE)
    }
  }

  private def verifyErrorResponse(nino: Nino, status: Int, baseError: BaseError): Assertion = {
    val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string
    val queryParams = detailQueryParams(IndividualDetails.fields)
    stubCall(HttpMethod.Get, path, status, errorResponseFromIF(), queryParams)

    val response = await(connector.getDetails(nino))

    verifyHeaders(HttpMethod.Get, path, queryParams)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
