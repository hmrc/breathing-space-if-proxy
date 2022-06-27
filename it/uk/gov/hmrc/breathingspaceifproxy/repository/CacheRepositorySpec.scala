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

package uk.gov.hmrc.breathingspaceifproxy.repository

import cats.implicits._
import play.api.Configuration
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.ErrorItem
import uk.gov.hmrc.breathingspaceifproxy.model.HashedNino
import uk.gov.hmrc.breathingspaceifproxy.model.Nino
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.support.BaseISpec
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CacheRepositorySpec extends BaseISpec with DefaultPlayMongoRepositorySupport[CacheItem] {

  override lazy val repository =
    new CacheRepository(mongoComponent, inject[Configuration], inject[TimestampSupport], inject[AppConfig], inject[HashedNinoCacheId])

  private val cacheId = HashedNino(Nino("AA000001A"))
  private val dataId = "memorandum"
  private val dataKey = DataKey[String](dataId)

  "fetch" should {
    "store value in mongo when it doesn't exist" in {
      repository.fetch(cacheId, dataId) { Future.successful("success value".validNec) }.futureValue

      val result = repository.get[String](cacheId)(dataKey).futureValue
      result shouldBe Some("success value")
    }

    "get value from mongo when it does exist" in {
      repository.put(cacheId)(dataKey, "success value").futureValue

      val result = repository.fetch(cacheId, dataId) { Future.successful("failure value".validNec) }.futureValue
      result shouldBe "success value".validNec
    }

    "don't store value in mongo when invalid response returned" in {
      val expected = ErrorItem(BaseError.INTERNAL_SERVER_ERROR).invalidNec[String]
      val fetchResult = repository.fetch(cacheId, dataId) { Future.successful(expected) }.futureValue
      val mongoResult = repository.get[String](cacheId)(dataKey).futureValue

      fetchResult shouldBe expected
      mongoResult shouldBe None
    }

    "don't store value when future fails" in {
      val expected = new Exception("thrown exception")
      val fetchResult = repository.fetch[String](cacheId, dataId) { Future.failed(expected) }.failed.futureValue
      val mongoResult = repository.get[String](cacheId)(dataKey).futureValue

      fetchResult shouldBe expected
      mongoResult shouldBe None
    }
  }
}