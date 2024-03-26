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

package uk.gov.hmrc.breathingspaceifproxy.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.Duration

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  lazy val appName: String = config.get[String]("appName")

  val integrationFrameworkBaseUrl: String = servicesConfig.baseUrl("integration-framework")

  val integrationFrameworkContext: String =
    config.get[String]("microservice.services.integration-framework.context")

  val integrationFrameworkEnvironment: String =
    config.get[String]("microservice.services.integration-framework.environment")

  val integrationFrameworkAuthToken =
    s"""Bearer ${config.get[String]("microservice.services.integration-framework.auth-token")}"""

  val httpHeaderCacheControl: String = config.get[String]("httpHeaders.cacheControl")

  // Must be 'lazy'
  lazy val v1AllowlistedApplicationIds: Seq[String] =
    config.get[Seq[String]]("api.access.version-1.0.allowlistedApplicationIds")

  val memorandumFeatureEnabled: Boolean = config.get[Boolean]("feature.flag.memorandum.enabled")

  object mongo {

    val ttl: Duration = config.get[Duration]("mongodb.ttl")
  }

  val ninoHashingKey: String = config.get[String]("ninoHashingKey")
}
