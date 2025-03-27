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

package uk.gov.hmrc

import scala.concurrent.Future

import cats.data.ValidatedNec
import uk.gov.hmrc.breathingspaceifproxy.model._

package object breathingspaceifproxy {

  val unit: Unit = ()

  val unattendedStaffPid = "0000000"

  type Validation[T]         = ValidatedNec[ErrorItem, T]
  type ResponseValidation[T] = Future[Validation[T]]

  object DownstreamHeader {
    lazy val CorrelationId = "Correlation-Id"
    lazy val RequestType   = "Request-Type"
    lazy val StaffPid      = "Pid"
    lazy val UpstreamState = "Upstream-State"
  }

  object UpstreamHeader {
    lazy val Authorization = "Authorization"
    lazy val Environment   = "Environment"
    lazy val CorrelationId = "CorrelationId"
    lazy val RequestType   = "OriginatorId"
    lazy val StaffPid      = "UserId"
  }
}
