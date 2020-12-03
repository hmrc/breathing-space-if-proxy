package uk.gov.hmrc.breathingspaceifproxy.connector

import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class IndividualDetailsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  val connector = inject[IndividualDetailsConnector]

  "get" should {
    "return a Detail0 instance when it receives the relative \"fields\" query parameter" in {
      val nino = genNino
      verifyResponse(nino, DetailData0, detail0(nino))
    }

    "return a IndividualDetails instance when the \"fields\" query parameter is not provided (full population)" in {
      val nino = genNino
      verifyResponse(nino, FullDetails, details(nino))
    }

    "return RESOURCE_NOT_FOUND when the provided resource is unknown" in {
      verifyErrorResponse(genNino, Status.NOT_FOUND, RESOURCE_NOT_FOUND)
    }

    "return CONFLICTING_REQUEST in case of duplicated requests" in {
      verifyErrorResponse(genNino, Status.CONFLICT, CONFLICTING_REQUEST)
    }

    "return SERVER_ERROR if the returned payload is unexpected" in {
      val nino = genNino

      val urlForDetail0 = urlWithoutQuery(IndividualDetailsConnector.path(nino, DetailData0.fields))
      val queryParamsForDetail0 = detailQueryParams(DetailData0.fields)

      val unexpectedPayload = Json.parse("""{"dateOfRegistration":"2020-01-01","sex":"M"}""").toString

      stubCall(HttpMethod.Get, urlForDetail0, Status.OK, unexpectedPayload, queryParamsForDetail0)

      val response = await(connector.get[Detail0](nino, DetailData0))

      verifyHeaders(HttpMethod.Get, urlForDetail0, queryParamsForDetail0)
      response.fold(_.head.baseError shouldBe SERVER_ERROR, _ => notAnErrorInstance)
    }

    "return SERVER_ERROR for any 4xx error, 404 and 409 excluded" in {
      verifyErrorResponse(genNino, Status.BAD_REQUEST, SERVER_ERROR)
    }

    "return DOWNSTREAM_BAD_GATEWAY for a 502(BAD_GATEWAY) error" in {
      verifyErrorResponse(genNino, Status.BAD_GATEWAY, DOWNSTREAM_BAD_GATEWAY)
    }

    "return DOWNSTREAM_SERVICE_UNAVAILABLE for a 503(SERVICE_UNAVAILABLE) error" in {
      verifyErrorResponse(genNino, Status.SERVICE_UNAVAILABLE, DOWNSTREAM_SERVICE_UNAVAILABLE)
    }

    "return DOWNSTREAM_TIMEOUT for a 504(GATEWAY_TIMEOUT) error" in {
      verifyErrorResponse(genNino, Status.GATEWAY_TIMEOUT, DOWNSTREAM_TIMEOUT)
    }

    "return SERVER_ERROR for any 5xx error (excluding 502,503,504)" in {
      verifyErrorResponse(genNino, Status.NOT_IMPLEMENTED, SERVER_ERROR)
    }
  }

  private def verifyResponse[T <: Detail](nino: Nino, detailData: DetailsData[T], detail: T): Assertion = {
    implicit val format: OFormat[T] = detailData.format

    val url = urlWithoutQuery(IndividualDetailsConnector.path(nino, detailData.fields))
    val queryParams = detailQueryParams(detailData.fields)

    stubCall(HttpMethod.Get, url, Status.OK, Json.toJson(detail).toString, queryParams)

    val response = await(connector.get[T](nino, detailData))

    verifyHeaders(HttpMethod.Get, url, queryParams)
    assert(response.fold(_ => false, _ => true))
  }

  private def verifyErrorResponse(nino: Nino, status: Int, baseError: BaseError): Assertion = {
    val url = urlWithoutQuery(IndividualDetailsConnector.path(nino, DetailData0.fields))
    val queryParams = detailQueryParams(DetailData0.fields)
    stubCall(HttpMethod.Get, url, status, errorResponsePayloadFromIF, queryParams)

    val response = await(connector.get[Detail0](nino, DetailData0))

    verifyHeaders(HttpMethod.Get, url, queryParams)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
