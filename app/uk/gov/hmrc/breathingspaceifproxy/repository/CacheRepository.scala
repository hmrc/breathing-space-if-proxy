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
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.mongo.cache.CacheIdType.SimpleCacheId
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class CacheRepository @Inject()(
  mongoComponent: MongoComponent,
  configuration: Configuration,
  timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "breathing-space-cache",
      ttl = 24.hours,
      timestampSupport = timestampSupport,
      cacheIdType = SimpleCacheId
    ) {

  def fetch[A: Writes: Reads](cacheId: String, dataId: String)(block: => Future[Validation[A]]): Future[Validation[A]] =
    get[A](cacheId)(DataKey(dataId)).flatMap {
      case Some(value) => Future.successful(value.validNec)
      case None =>
        for {
          value <- block
          _ <- value.fold(
            _ => Future.successful(()),
            put(cacheId)(DataKey(dataId), _).map(_ => ())
          )
        } yield value
    }
}
