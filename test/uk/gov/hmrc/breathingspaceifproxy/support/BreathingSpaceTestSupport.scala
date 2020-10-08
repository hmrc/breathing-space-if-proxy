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

package uk.gov.hmrc.breathingspaceifproxy.support

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import cats.syntax.option._
import cats.syntax.validated._
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http.HeaderCarrier

final case class PostPeriodsRequest(nino: String, periods: PostPeriodsInRequest)

object PostPeriodsRequest {
  implicit val format = Json.format[PostPeriodsRequest]
}

trait BreathingSpaceTestSupport {

  def appConfig: AppConfig

  val validNinoAsString = "MZ006526C"
  val nino = Nino(validNinoAsString)
  val unknownNino = Nino("MZ005527C")
  val invalidNino = "MG34567"

  val randomUUID = UUID.randomUUID
  val randomUUIDAsString = randomUUID.toString
  val correlationId = randomUUID
  val correlationIdAsString = randomUUIDAsString
  val periodId = randomUUID
  val periodIdAsString = randomUUIDAsString

  val attendedStaffPid = "1234567"

  val errorResponsePayloadFromIF = """{"failures":[{"code":"AN_ERROR","message":"An error message"}]}"""

  implicit val genericRequestId = RequestId(EndpointId.Breathing_Space_Periods_POST, correlationId)

  lazy val requestHeaders = List(
    CONTENT_TYPE -> MimeTypes.JSON,
    Header.CorrelationId -> correlationIdAsString,
    Header.RequestType -> Attended.DS2_BS_ATTENDED.toString,
    Header.StaffPid -> attendedStaffPid
  )

  lazy val requestHeadersForUnattended = List(
    CONTENT_TYPE -> MimeTypes.JSON,
    Header.CorrelationId -> correlationIdAsString,
    Header.RequestType -> Attended.DS2_BS_UNATTENDED.toString,
    Header.StaffPid -> unattendedStaffPid
  )

  implicit lazy val headerCarrierForIF = HeaderCarrier(
    extraHeaders = List(
      CONTENT_TYPE -> MimeTypes.JSON,
      retrieveHeaderMapping(Header.CorrelationId) -> correlationIdAsString,
      retrieveHeaderMapping(Header.RequestType) -> Attended.DS2_BS_ATTENDED.entryName,
      retrieveHeaderMapping(Header.StaffPid) -> attendedStaffPid
    )
  )

  lazy val validPostPeriod = PostPeriodInRequest(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(1).some,
    ZonedDateTime.now
  )

  lazy val invalidPostPeriod = PostPeriodInRequest(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(4).some,
    ZonedDateTime.now
  )

  lazy val postPeriodsRequest: PostPeriodsInRequest = List(validPostPeriod, validPostPeriod)

  lazy val validPutPeriod = PutPeriodInRequest(
    UUID.randomUUID(),
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(1).some,
    ZonedDateTime.now
  )

  lazy val invalidPutPeriod = PutPeriodInRequest(
    UUID.randomUUID(),
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(4).some,
    ZonedDateTime.now
  )

  lazy val putPeriodsRequest: PutPeriodsInRequest = List(validPutPeriod, validPutPeriod)

  lazy val validPeriodsResponse =
    PeriodsInResponse(
      List(
        PeriodInResponse(UUID.randomUUID(), LocalDate.now.minusMonths(5), None),
        PeriodInResponse(UUID.randomUUID(), LocalDate.now.minusMonths(2), LocalDate.now.some)
      )
    )

  lazy val fakeGetRequest = FakeRequest().withHeaders(requestHeaders: _*)

  def correlationIdAsOpt(withCorrelationId: => Boolean): Option[String] =
    if (withCorrelationId) correlationIdAsString.some else None

  def requestWithAllHeaders(method: String = "GET"): FakeRequest[AnyContentAsEmpty.type] =
    requestFilteredOutOneHeader("", method)

  def requestFilteredOutOneHeader(
    headerToFilterOut: String,
    method: String = "GET"
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "/").withHeaders(
      requestHeaders.filter(_._1.toLowerCase != headerToFilterOut.toLowerCase): _*
    )

  def postPeriodsRequestAsJson(postPeriods: PostPeriodsInRequest): JsValue =
    Json.toJson(PostPeriodsRequest(validNinoAsString, postPeriods))

  def postPeriodsRequestAsJson(nino: String, postPeriods: PostPeriodsInRequest): JsValue =
    Json.toJson(PostPeriodsRequest(nino, postPeriods))

  def postPeriodsRequest(postPeriods: PostPeriodsInRequest): Validation[JsValue] =
    postPeriodsRequestAsJson(postPeriods).validNec[ErrorItem]

  def postPeriodsRequest(
    nino: String,
    startDate: String,
    endDate: Option[String],
    timestamp: String
  ): Validation[JsValue] = {
    val sd = s""""$startDateKey":"$startDate""""
    val ed = endDate.fold("")(v => s""","$endDateKey":"$v"""")
    val ts = s""""$timestampKey":"$timestamp""""

    Json.parse(s"""{"nino":"$nino","periods":[{$sd$ed,$ts}]}""").validNec[ErrorItem]
  }

  def putPeriodsRequestAsJson(putPeriods: PutPeriodsInRequest): JsValue =
    Json.obj("periods" -> putPeriods)

  def putPeriodsRequest(putPeriods: PutPeriodsInRequest): Validation[JsValue] =
    putPeriodsRequestAsJson(putPeriods).validNec[ErrorItem]

  def putPeriodsRequest(
    periodId: String,
    startDate: String,
    endDate: Option[String],
    timestamp: String
  ): Validation[JsValue] = {
    val pi = s""""$periodIdKey":"$periodId""""
    val sd = s""""$startDateKey":"$startDate""""
    val ed = endDate.fold("")(v => s""","$endDateKey":"$v"""")
    val ts = s""""$timestampKey":"$timestamp""""

    Json.parse(s"""{"periods":[{$pi,$sd$ed,$ts}]}""").validNec[ErrorItem]
  }

  def debtorDetails(nino: Nino): String =
    s"""
       |{"nino" : "${nino.value}",
       | "firstName" : "John",
       | "lastName" : "Smith",
       | "dateOfBirth" : "1990-01-01",
       |}
     """.stripMargin

  def retrieveHeaderMapping(header: String): String =
    appConfig.headerMapping.filter(_.nameToMap == header).head.nameMapped
}
