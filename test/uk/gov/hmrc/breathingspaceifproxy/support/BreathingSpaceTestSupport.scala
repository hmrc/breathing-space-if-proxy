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

package uk.gov.hmrc.breathingspaceifproxy.support

import cats.syntax.option._
import cats.syntax.validated._
import play.api.http.HeaderNames.{AUTHORIZATION, CONTENT_TYPE}
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.service.UpstreamConnector
import uk.gov.hmrc.breathingspaceifproxy.model.Nino.{validPrefixes, validSuffixes}
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.{Attended, EndpointId}
import uk.gov.hmrc.breathingspaceifproxy._

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID
import scala.util.Random

final case class PostPeriodsRequest(
  nino: String,
  consumerRequestId: UUID,
  utr: Option[String],
  periods: List[PostPeriodInRequest]
)

object PostPeriodsRequest {
  implicit val format: OFormat[PostPeriodsRequest] = Json.format[PostPeriodsRequest]
}

trait BreathingSpaceTestSupport {

  def appConfig: AppConfig

  val invalidNino = "MG34567"

  val randomUUID: UUID              = UUID.randomUUID
  val randomUUIDAsString: String    = randomUUID.toString
  val correlationId: UUID           = randomUUID
  val correlationIdAsString: String = randomUUIDAsString
  val periodId: UUID                = randomUUID
  val periodIdAsString: String      = randomUUIDAsString

  val attendedStaffPid = "1234567"

  lazy val requestHeaders: List[(String, String)] = List(
    CONTENT_TYPE                   -> MimeTypes.JSON,
    DownstreamHeader.CorrelationId -> correlationIdAsString,
    DownstreamHeader.RequestType   -> Attended.DA2_BS_ATTENDED.toString,
    DownstreamHeader.StaffPid      -> attendedStaffPid,
    AUTHORIZATION                  -> "Bearer 12345"
  )

  lazy val requestHeadersForUnattended: List[(String, String)] = List(
    CONTENT_TYPE                   -> MimeTypes.JSON,
    DownstreamHeader.CorrelationId -> correlationIdAsString,
    DownstreamHeader.RequestType   -> Attended.DA2_BS_UNATTENDED.toString,
    DownstreamHeader.StaffPid      -> unattendedStaffPid,
    AUTHORIZATION                  -> "Bearer 12345"
  )

  lazy val requestHeadersForMemorandum: List[(String, String)] = List(
    CONTENT_TYPE                   -> MimeTypes.JSON,
    DownstreamHeader.CorrelationId -> correlationIdAsString,
    AUTHORIZATION                  -> "Bearer 12345"
  )

  lazy val validPostPeriod: PostPeriodInRequest = PostPeriodInRequest(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(1).some,
    ZonedDateTime.now
  )

  lazy val invalidPostPeriod: PostPeriodInRequest = PostPeriodInRequest(
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(4).some,
    ZonedDateTime.now
  )

  lazy val validPutPeriod: PutPeriodInRequest = PutPeriodInRequest(
    UUID.randomUUID(),
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(1).some,
    ZonedDateTime.now
  )

  lazy val invalidPutPeriod: PutPeriodInRequest = PutPeriodInRequest(
    UUID.randomUUID(),
    LocalDate.now.minusMonths(3),
    LocalDate.now.minusMonths(4).some,
    ZonedDateTime.now
  )

  lazy val putPeriodsRequest: List[PutPeriodInRequest] = List(validPutPeriod, validPutPeriod)

  lazy val validPeriodsResponse: PeriodsInResponse =
    PeriodsInResponse(
      List(
        PeriodInResponse(UUID.randomUUID(), LocalDate.now.minusMonths(5), None),
        PeriodInResponse(UUID.randomUUID(), LocalDate.now.minusMonths(2), LocalDate.now.some)
      )
    )

  lazy val debt1: Debt = Debt(
    chargeReference = "ETMP ref01",
    chargeDescription = "100 chars long charge description as exist in ETMP",
    chargeAmount = 199999999.11,
    chargeCreationDate = LocalDate.now,
    chargeDueDate = LocalDate.now.plusMonths(1),
    none
  )

  lazy val debt2: Debt = Debt(
    chargeReference = "ETMP ref02",
    chargeDescription = "long charge 02 description as exist in ETMP",
    chargeAmount = 299999999.22,
    chargeCreationDate = LocalDate.now.plusDays(2),
    chargeDueDate = LocalDate.now.plusMonths(2),
    utrAssociatedWithCharge = "1234567890".some
  )

  lazy val listOfDebts: List[Debt]    = List(debt1, debt2)
  lazy val debtsAsSentFromEis: String = Json.toJson(listOfDebts).toString
  lazy val debts: String              = Json.toJson(Debts(listOfDebts)).toString

  lazy val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type]           = FakeRequest().withHeaders(requestHeaders: _*)
  lazy val fakeUnAttendedGetRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders(requestHeadersForUnattended: _*)
  lazy val fakeMemorandumGetRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders(requestHeadersForMemorandum: _*)

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

  def genRequestId(endpointId: EndpointId, upstreamConnector: UpstreamConnector): RequestId =
    RequestId(endpointId, correlationId, Attended.DA2_BS_ATTENDED, attendedStaffPid, upstreamConnector)

  def genUnattendedRequestId(endpointId: EndpointId, upstreamConnector: UpstreamConnector): RequestId =
    RequestId(endpointId, correlationId, Attended.DA2_BS_UNATTENDED, unattendedStaffPid, upstreamConnector)

  def attendedRequestWithAllHeaders(method: String = "GET"): FakeRequest[AnyContentAsEmpty.type] =
    attendedRequestFilteredOutOneHeader("", method)

  def attendedRequestFilteredOutOneHeader(
    headerToFilterOut: String,
    method: String = "GET"
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "/").withHeaders(
      requestHeaders.filter(_._1.toLowerCase != headerToFilterOut.toLowerCase): _*
    )

  def memorandumRequestFilteredOutOneHeader(
    headerToFilterOut: String,
    method: String = "GET"
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "/").withHeaders(
      requestHeadersForMemorandum.filter(_._1.toLowerCase != headerToFilterOut.toLowerCase): _*
    )

  def unattendedRequestWithAllHeaders(method: String = "GET"): FakeRequest[AnyContentAsEmpty.type] =
    unattendedRequestFilteredOutOneHeader("", method)

  def unattendedRequestFilteredOutOneHeader(
    headerToFilterOut: String,
    method: String = "GET"
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "/").withHeaders(
      requestHeadersForUnattended.filter(_._1.toLowerCase != headerToFilterOut.toLowerCase): _*
    )

  def postPeriodsRequest(utr: Option[String] = "9876543210".some): PostPeriodsInRequest =
    PostPeriodsInRequest(randomUUID, utr, List(validPostPeriod, validPostPeriod))

  def postPeriodsRequestAsJson(nino: String, postPeriods: PostPeriodsInRequest): JsValue =
    Json.toJson(PostPeriodsRequest(nino, randomUUID, postPeriods.utr, postPeriods.periods))

  def postPeriodsRequestAsJson(postPeriods: PostPeriodsInRequest): Validation[JsValue] =
    Json.toJson(PostPeriodsRequest(genNinoString, randomUUID, postPeriods.utr, postPeriods.periods)).validNec[ErrorItem]

  def putPeriodsRequestAsJson(putPeriods: List[PutPeriodInRequest]): JsValue =
    Json.obj("periods" -> putPeriods)

  def putPeriodsRequest(putPeriods: List[PutPeriodInRequest]): Validation[JsValue] =
    putPeriodsRequestAsJson(putPeriods).validNec[ErrorItem]

  def details(nino: Nino): IndividualDetails = IndividualDetails(
    details = Details(
      nino = nino.value,
      dateOfBirth = LocalDate.now.some
    ),
    nameList = NameList(
      List(
        NameData(
          firstForename = "Mickey".some,
          surname = "Mouse".some,
          nameType = 1.some
        )
      )
    ).some,
    addressList = AddressList(
      List(
        AddressData(
          addressLine1 = "Some Street".some,
          countryCode = 1.some,
          addressPostcode = "E14".some,
          addressType = 1.some
        )
      )
    ).some,
    indicators = Indicators(welshOutputInd = 1.some).some
  )

  def detailQueryParams(fields: String): Map[String, String] =
    fields
      .split("[?=]")
      .tail
      .grouped(2)
      .foldLeft(Map.empty[String, String]) { (map, pair) =>
        map + (pair(0) -> pair(1))
      }
}
