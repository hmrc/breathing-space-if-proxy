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

import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.Assertion
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model.{HashedNino, MemorandumInResponse}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.{CONFLICTING_REQUEST, RESOURCE_NOT_FOUND}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Memorandum_GET
import uk.gov.hmrc.breathingspaceifproxy.repository.CacheRepository
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class MemorandumConnectorISpec extends BaseISpec with ConnectorTestSupport with DefaultPlayMongoRepositorySupport[CacheItem] {

  override val fakeApplication: Application =
    GuiceApplicationBuilder()
      .configure(configProperties)
      .overrides(bind[MongoComponent].to(mongoComponent))
      .build()

  override lazy val repository = inject[CacheRepository]
  val connector = inject[MemorandumConnector]
  implicit val requestId = genRequestId(BS_Memorandum_GET, connector.eisConnector)

  "get" should {
    "return an MemorandumInResponse instance when it receives a 200(OK) response" in {

      val expectedBreathingSpaceIndicator = true
      val memorandum = MemorandumInResponse(expectedBreathingSpaceIndicator)

      val nino = genNino
      val url = MemorandumConnector.path(nino)
      val responsePayload = Json.toJson(memorandum).toString
      stubCall(HttpMethod.Get, url, OK, responsePayload)

      val response = await(connector.get(nino))

      verifyHeaders(HttpMethod.Get, url)
      response.fold(
        _    => fail(),
        resp => resp.breathingSpaceIndicator shouldBe expectedBreathingSpaceIndicator
      )
    }

    "return cached response when available" in {
      val nino = genNino
      val expectedBreathingSpaceIndicator = true
      repository.put(HashedNino(nino))(DataKey("memorandum"), MemorandumInResponse(expectedBreathingSpaceIndicator))

      val memorandum = MemorandumInResponse(false)

      val url = MemorandumConnector.path(nino)
      val responsePayload = Json.toJson(memorandum).toString
      stubCall(HttpMethod.Get, url, OK, responsePayload)

      val response = await(connector.get(nino))

      WireMock.verify(0, HttpMethod.Get.requestedFor(WireMock.urlPathEqualTo(url)))
      response.fold(
        _    => fail(),
        resp => resp.breathingSpaceIndicator shouldBe expectedBreathingSpaceIndicator
      )
    }

    "return RESOURCE_NOT_FOUND when the provided resource is unknown" in {
      verifyGetResponse(NOT_FOUND, RESOURCE_NOT_FOUND)
    }

    "return CONFLICTING_REQUEST in case of duplicated requests" in {
      verifyGetResponse(CONFLICT, CONFLICTING_REQUEST)
    }

    "return SERVER_ERROR for any 4xx error, 404 and 409 excluded" in {
      verifyGetResponse(BAD_REQUEST, BaseError.INTERNAL_SERVER_ERROR)
    }

    "return SERVER_ERROR for any 5xx error, 502, 503 and 504 excluded" in {
      verifyGetResponse(NOT_IMPLEMENTED, BaseError.SERVER_ERROR)
    }
  }

  private def verifyGetResponse(status: Int, baseError: BaseError): Assertion = {
    val nino = genNino
    val url = MemorandumConnector.path(nino)
    stubCall(HttpMethod.Get, url, status, errorResponseFromIF())

    val response = await(connector.get(nino))

    verifyHeaders(HttpMethod.Get, url)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
