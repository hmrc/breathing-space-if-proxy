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

import scala.util.Random

import cats.syntax.option._
import cats.syntax.validated._
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.breathingspaceifproxy._
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.UpstreamConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.Nino.{validPrefixes, validSuffixes}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.{Attended, EndpointId}

final case class PostPeriodsRequest(nino: String, periods: PostPeriodsInRequest)

object PostPeriodsRequest {
  implicit val format = Json.format[PostPeriodsRequest]
}

trait BreathingSpaceTestSupport {

  def appConfig: AppConfig

  val invalidNino = "MG34567"

  val randomUUID = UUID.randomUUID
  val randomUUIDAsString = randomUUID.toString
  val correlationId = randomUUID
  val correlationIdAsString = randomUUIDAsString
  val periodId = randomUUID
  val periodIdAsString = randomUUIDAsString

  val attendedStaffPid = "1234567"

  lazy val requestHeaders = List(
    CONTENT_TYPE -> MimeTypes.JSON,
    Header.CorrelationId -> correlationIdAsString,
    Header.RequestType -> Attended.DA2_BS_ATTENDED.toString,
    Header.StaffPid -> attendedStaffPid
  )

  lazy val requestHeadersForUnattended = List(
    CONTENT_TYPE -> MimeTypes.JSON,
    Header.CorrelationId -> correlationIdAsString,
    Header.RequestType -> Attended.DA2_BS_UNATTENDED.toString,
    Header.StaffPid -> unattendedStaffPid
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

  lazy val debt1 = Debt(
    chargeReference = "ETMP ref01",
    chargeDescription = "100 chars long charge description as exist in ETMP",
    chargeAmount = 199999999.11,
    chargeCreationDate = LocalDate.now,
    chargeDueDate = LocalDate.now.plusMonths(1),
    none
  )

  lazy val debt2 = Debt(
    chargeReference = "ETMP ref02",
    chargeDescription = "long charge 02 description as exist in ETMP",
    chargeAmount = 299999999.22,
    chargeCreationDate = LocalDate.now.plusDays(2),
    chargeDueDate = LocalDate.now.plusMonths(2),
    utrAssociatedWithCharge = "1234567890".some
  )

  lazy val debts = List(debt1, debt2)

  lazy val fakeGetRequest = FakeRequest().withHeaders(requestHeaders: _*)

  lazy val random: Random = new Random

  def correlationIdAsOpt(withCorrelationId: => Boolean): Option[String] =
    if (withCorrelationId) correlationIdAsString.some else None

  def errorResponseFromIF(code: String = "AN_ERROR"): String =
    s"""{"failures":[{"code":"$code","reason":"An error message"}]}"""

  def genNino: Nino = {
    val prefix = validPrefixes(random.nextInt(validPrefixes.length))
    val number = random.nextInt(1000000)
    Nino(f"$prefix$number%06d")
  }

  def genNinoString: String = genNino.value

  def genNinoWithSuffix: Nino = {
    val prefix = validPrefixes(random.nextInt(validPrefixes.length))
    val number = random.nextInt(1000000)
    val suffix = validSuffixes(random.nextInt(validSuffixes.length))
    Nino(f"$prefix$number%06d$suffix")
  }

  def genRequestId(endpointId: EndpointId, uc: UpstreamConnector): RequestId =
    RequestId(endpointId, correlationId, attendedStaffPid, uc)

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
    Json.toJson(PostPeriodsRequest(genNinoString, postPeriods))

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

  def details(nino: Nino): IndividualDetails = IndividualDetails(
    details = Details(
      nino = nino.value,
      dateOfBirth = LocalDate.now.some
    ),
    nameList = NameList(
      List(
        NameData(
          firstForename = "Mickey".some,
          surname = "Mouse".some
        )
      )
    ).some,
    addressList = AddressList(
      List(
        AddressData(
          addressLine1 = "Some Street".some,
          countryCode = 1.some,
          addressPostcode = "E14".some
        )
      )
    ).some,
    indicators = Indicators(welshOutputInd = 1.some).some
  )

  def detailQueryParams(fields: String): Map[String, String] =
    fields
      .split("\\?|=")
      .tail
      .grouped(2)
      .foldLeft(Map.empty[String, String]) { (map, pair) =>
        map + (pair(0) -> pair(1))
      }

  def retrieveHeaderMapping(header: String): String =
    appConfig.headerMapping.filter(_.nameToMap == header).head.nameMapped
}
