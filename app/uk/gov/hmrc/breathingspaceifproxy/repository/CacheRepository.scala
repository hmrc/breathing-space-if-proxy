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

package uk.gov.hmrc.breathingspaceifproxy.repository

import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.breathingspaceifproxy.Validation
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.HashedNino
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HashedNinoCacheId @Inject() (implicit appConfig: AppConfig) extends CacheIdType[HashedNino] {
  override def run: HashedNino => String = _.generateHash()
}

@Singleton
class CacheRepository @Inject() (
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport,
  appConfig: AppConfig,
  cacheIdType: HashedNinoCacheId
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "breathing-space-cache",
      ttl = appConfig.mongo.ttl,
      timestampSupport = timestampSupport,
      cacheIdType = cacheIdType
    ) {

  def fetch[A: Writes: Reads](nino: HashedNino, endpoint: String)(
    block: => Future[Validation[A]]
  ): Future[Validation[A]] =
    get[A](nino)(DataKey(endpoint)).flatMap {
      case Some(value) => Future.successful(value.validNec)
      case None        =>
        for {
          value <- block
          _     <- value.fold(
                     _ => Future.successful(()),
                     put(nino)(DataKey(endpoint), _).map(_ => ())
                   )
        } yield value
    }

  def clear(nino: HashedNino): Future[Unit] =
    deleteEntity(nino)
}
