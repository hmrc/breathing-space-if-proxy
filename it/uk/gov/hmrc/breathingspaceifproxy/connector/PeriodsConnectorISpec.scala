package uk.gov.hmrc.breathingspaceifproxy.connector

import cats.syntax.option._
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.Header
import uk.gov.hmrc.breathingspaceifproxy.model.Attended
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

class PeriodsConnectorISpec extends BaseISpec {

    implicit lazy val headerCarrierForIF = HeaderCarrier(
      authorization = Authorization(appConfig.integrationframeworkAuthToken).some,
      extraHeaders = List(
        CONTENT_TYPE -> MimeTypes.JSON,
        Header.Environment -> appConfig.integrationFrameworkEnvironment,
        retrieveHeaderMapping(Header.CorrelationId) -> correlationIdAsString,
        retrieveHeaderMapping(Header.RequestType) -> Attended.DS2_BS_ATTENDED.entryName,
        retrieveHeaderMapping(Header.StaffPid) -> attendedStaffPid
      )
    )

  val connector = inject[PeriodsConnector]

  lazy val notAnErrorInstance = assert(false, "Not even an Error instance?")

  "get" should {
    "return a PeriodsResponse instance when it receives a 200(OK) response" in {
      val responsePayload = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Get, periodsConnectorUrl, Status.OK, responsePayload)
      val response = await(connector.get(nino))
      verifyHeaders(HttpMethod.Get, periodsConnectorUrl)
      assert(response.fold(_ => false, _ => true))
    }

    "return RESOURCE_NOT_FOUND when the provided resource is unknown" in {
      val url = PeriodsConnector.path(unknownNino)

      stubCall(HttpMethod.Get, url, Status.NOT_FOUND, errorResponsePayloadFromIF)
      val response = await(connector.get(unknownNino))
      verifyHeaders(HttpMethod.Get, url)
      response.fold(_.head.baseError shouldBe RESOURCE_NOT_FOUND, _ => notAnErrorInstance)
    }

    "return SERVER_ERROR for any 4xx error, 404 excluded" in {
      stubCall(HttpMethod.Get, periodsConnectorUrl, Status.BAD_REQUEST, errorResponsePayloadFromIF)
      val response = await(connector.get(nino))
      verifyHeaders(HttpMethod.Get, periodsConnectorUrl)
      response.fold(_.head.baseError shouldBe SERVER_ERROR, _ => notAnErrorInstance)
    }

    "return SERVER_ERROR for any 5xx error, (500,502,503) excluded" in {
      stubCall(HttpMethod.Get, periodsConnectorUrl, Status.BAD_GATEWAY, errorResponsePayloadFromIF)
      val response = await(connector.get(nino))
      verifyHeaders(HttpMethod.Get, periodsConnectorUrl)
      response.fold(_.head.baseError shouldBe SERVER_ERROR, _ => notAnErrorInstance)
    }
  }

  "post" should {
    "return a PeriodsResponse instance when it receives a 201(CREATED) response" in {
      val responsePayload = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Post, periodsConnectorUrl, Status.CREATED, responsePayload)

      val response = await(connector.post(nino, postPeriodsRequest))
      verifyHeaders(HttpMethod.Post, periodsConnectorUrl)
      assert(response.fold(_ => false, _ => true))
    }
  }

  "put" should {
    "return a PeriodsResponse instance when it receives a 200(OK) response" in {
      val responsePayload = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Put, periodsConnectorUrl, Status.OK, responsePayload)

      val response = await(connector.put(nino, putPeriodsRequest))
      verifyHeaders(HttpMethod.Put, periodsConnectorUrl)
      assert(response.fold(_ => false, _ => true))
    }
  }
}
