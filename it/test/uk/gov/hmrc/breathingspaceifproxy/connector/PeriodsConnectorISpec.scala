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

package uk.gov.hmrc.breathingspaceifproxy.connector

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Periods_GET
import uk.gov.hmrc.breathingspaceifproxy.model.{HashedNino, MemorandumInResponse, RequestId}
import uk.gov.hmrc.breathingspaceifproxy.repository.CacheRepository
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class PeriodsConnectorISpec
    extends BaseISpec
    with ConnectorTestSupport
    with DefaultPlayMongoRepositorySupport[CacheItem] {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(configProperties)
      .overrides(bind[MongoComponent].to(mongoComponent))
      .build()

  override val repository: CacheRepository = inject[CacheRepository]
  val connector: PeriodsConnector          = inject[PeriodsConnector]
  implicit val requestId: RequestId        = genRequestId(BS_Periods_GET, connector.eisConnector)

  "get" should {
    "return a PeriodsResponse instance when it receives a 200(OK) response" in {
      val nino            = genNino
      val url             = PeriodsConnector.path(nino)
      val responsePayload = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Get, url, OK, responsePayload)

      val response = await(connector.get(nino))

      verifyHeaders(HttpMethod.Get, url)
      assert(response.fold(_ => false, _ => true))
    }

    "return RESOURCE_NOT_FOUND when the provided resource is unknown" in {
      verifyGetResponse(NOT_FOUND, RESOURCE_NOT_FOUND, "RESOURCE_NOT_FOUND".some)
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

  "post" should {
    "return a PeriodsResponse instance when it receives a 201(CREATED) response" in {
      val nino            = genNino
      val url             = PeriodsConnector.path(nino)
      val responsePayload = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Post, url, CREATED, responsePayload)

      val response = await(connector.post(nino, postPeriodsRequest()))
      verifyHeaders(HttpMethod.Post, url)
      assert(response.fold(_ => false, _ => true))
    }

    "clear cache for nino when called" in {
      val nino            = genNino
      repository.put(HashedNino(nino))(DataKey("memorandum"), MemorandumInResponse(true))
      val url             = PeriodsConnector.path(nino)
      val responsePayload = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Post, url, CREATED, responsePayload)

      await(connector.post(nino, postPeriodsRequest()))

      val cachedValue =
        await(repository.get[MemorandumInResponse](HashedNino(nino))(DataKey("memorandum")))

      cachedValue shouldBe None
    }
  }

  "put" should {
    "return a PeriodsResponse instance when it receives a 200(OK) response" in {
      val nino            = genNino
      val url             = PeriodsConnector.path(nino)
      val responsePayload = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Put, url, OK, responsePayload)

      val response = await(connector.put(nino, putPeriodsRequest))
      verifyHeaders(HttpMethod.Put, url)
      assert(response.fold(_ => false, _ => true))
    }

    "clear cache for nino when called" in {
      val nino            = genNino
      repository.put(HashedNino(nino))(DataKey("memorandum"), MemorandumInResponse(true))
      val url             = PeriodsConnector.path(nino)
      val responsePayload = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Put, url, OK, responsePayload)

      await(connector.put(nino, putPeriodsRequest))

      val cachedValue =
        await(repository.get[MemorandumInResponse](HashedNino(nino))(DataKey("memorandum")))

      cachedValue shouldBe None
    }
  }

  private def verifyGetResponse(status: Int, baseError: BaseError, code: Option[String] = None): Assertion = {
    val nino = genNino
    val url  = PeriodsConnector.path(nino)
    stubCall(HttpMethod.Get, url, status, errorResponseFromIF(code.fold(baseError.entryName)(identity)))

    val response = await(connector.get(nino))

    verifyHeaders(HttpMethod.Get, url)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
