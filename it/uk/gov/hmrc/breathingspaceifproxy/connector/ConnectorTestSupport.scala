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

package uk.gov.hmrc.breathingspaceifproxy.connector

import cats.syntax.option._
import play.api.http.MimeTypes
import uk.gov.hmrc.breathingspaceifproxy.UpstreamHeader
import uk.gov.hmrc.breathingspaceifproxy.model.enums.Attended
import uk.gov.hmrc.breathingspaceifproxy.support.BaseISpec
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

trait ConnectorTestSupport { this: BaseISpec =>

  implicit lazy val headerCarrierForIF = HeaderCarrier(
    authorization = Authorization(appConfig.integrationframeworkAuthToken).some,
    extraHeaders = List(
      CONTENT_TYPE -> MimeTypes.JSON,
      UpstreamHeader.Environment -> appConfig.integrationFrameworkEnvironment,
      UpstreamHeader.CorrelationId -> correlationIdAsString,
      UpstreamHeader.RequestType -> Attended.DA2_BS_ATTENDED.entryName,
      UpstreamHeader.StaffPid -> attendedStaffPid
    )
  )

  lazy val notAnErrorInstance = assert(false, "Not even an Error instance?")
}
