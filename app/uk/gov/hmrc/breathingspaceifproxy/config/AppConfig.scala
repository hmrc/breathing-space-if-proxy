/*
 * Copyright 2020 HM Revenue & Customs
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

import java.net.URL
import javax.inject.{Inject, Singleton}

import play.api.Configuration
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")

  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  val integrationFrameworkBaseUrl: String = servicesConfig.baseUrl("integration-framework")
  val integrationFrameworkContext: String = config.get[String]("microservice.services.integration-framework.context")
  val integrationFrameworkUrl = new URL(s"$integrationFrameworkBaseUrl/$integrationFrameworkContext").toString

  lazy val v1WhitelistedApplicationIds =
    config.get[Seq[String]]("api.access.version-1.0.whitelistedApplicationIds")

  lazy val headerMapping = Map[String, String](
    Header.CorrelationId -> servicesConfig.getString("mapping.if.correlation-id"),
    Header.RequestType -> servicesConfig.getString("mapping.if.request-type"),
    Header.StaffId -> servicesConfig.getString("mapping.if.staff-id")
  )
}
