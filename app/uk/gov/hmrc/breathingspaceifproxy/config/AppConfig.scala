/*
 * Copyright 2021 HM Revenue & Customs
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

final case class HeaderMapping(nameToMap: String, nameMapped: String)

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  lazy val appName = config.get[String]("appName")

  lazy val onDevEnvironment: Boolean =
    config.getOptional[String]("environment.id").fold(false)(_.toLowerCase == "development")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")

  lazy val numberOfCallsToTriggerStateChange =
    if (onDevEnvironment) Int.MaxValue // Disable the Circuit Breaker on Dev
    else config.get[Int]("circuit.breaker.failedCallsInUnstableBeforeUnavailable")

  lazy val unavailablePeriodDuration = config.get[Int]("circuit.breaker.unavailablePeriodDurationInMillis")
  lazy val unstablePeriodDuration = config.get[Int]("circuit.breaker.unstablePeriodDurationInMillis")

  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  val integrationFrameworkBaseUrl: String = servicesConfig.baseUrl("integration-framework")

  val integrationFrameworkContext: String =
    config.get[String]("microservice.services.integration-framework.context")

  val integrationFrameworkEnvironment: String =
    config.get[String]("microservice.services.integration-framework.environment")

  val integrationframeworkAuthToken =
    s"""Bearer ${config.get[String]("microservice.services.integration-framework.auth-token")}"""

  // Must be 'lazy'
  lazy val v1AllowlistedApplicationIds =
    config.get[Seq[String]]("api.access.version-1.0.allowlistedApplicationIds")
}
