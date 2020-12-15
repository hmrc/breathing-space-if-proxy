package uk.gov.hmrc.breathingspaceifproxy.connector

import org.scalatest.Assertion
import play.api.http.Status._
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.ResponseValidation
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Detail0_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class IndividualDetailsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  implicit val requestId = genRequestId(BS_Detail0_GET)
  val connector = inject[IndividualDetailsConnector]

  "get" should {
    "return a Detail0 instance when it receives the relative \"fields\" query parameter" in {
      val nino = genNino
      verifyResponse(nino, IndividualDetail0, detail0(nino), connector.getDetail0(_))
    }

    "return a IndividualDetails instance when the \"fields\" query parameter is not provided (full population)" in {
      val nino = genNino
      verifyResponse(nino, IndividualDetails, details(nino), connector.getDetails(_))
    }

    "return RESOURCE_NOT_FOUND when the provided resource is unknown" in {
      verifyErrorResponse(genNino, NOT_FOUND, RESOURCE_NOT_FOUND)
    }

    "return CONFLICTING_REQUEST in case of duplicated requests" in {
      verifyErrorResponse(genNino, CONFLICT, CONFLICTING_REQUEST)
    }

    "return SERVER_ERROR if the returned payload is unexpected" in {
      val nino = genNino
      val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string

      val unexpectedPayload = Json.parse("""{"dateOfRegistration":"2020-01-01","sex":"M"}""").toString
      val queryParamsForDetail0 = detailQueryParams(IndividualDetail0.fields)

      stubCall(HttpMethod.Get, path, OK, unexpectedPayload, queryParamsForDetail0)

      val response = await(connector.getDetail0(nino))

      verifyHeaders(HttpMethod.Get, path, queryParamsForDetail0)
      response.fold(_.head.baseError shouldBe SERVER_ERROR, _ => notAnErrorInstance)
    }

    "return SERVER_ERROR for any 4xx error, 404 and 409 excluded" in {
      verifyErrorResponse(genNino, BAD_REQUEST, SERVER_ERROR)
    }

    "return UPSTREAM_BAD_GATEWAY for a 502(BAD_GATEWAY) error" in {
      verifyErrorResponse(genNino, BAD_GATEWAY, UPSTREAM_BAD_GATEWAY)
    }

    "return UPSTREAM_SERVICE_UNAVAILABLE for a 503(SERVICE_UNAVAILABLE) error" in {
      verifyErrorResponse(genNino, SERVICE_UNAVAILABLE, UPSTREAM_SERVICE_UNAVAILABLE)
    }

    "return UPSTREAM_TIMEOUT for a 504(GATEWAY_TIMEOUT) error" in {
      verifyErrorResponse(genNino, GATEWAY_TIMEOUT, UPSTREAM_TIMEOUT)
    }

    "return SERVER_ERROR for any 5xx error (excluding 502,503,504)" in {
      verifyErrorResponse(genNino, NOT_IMPLEMENTED, SERVER_ERROR)
    }
  }

  private def verifyResponse[T <: Detail](
    nino: Nino,
    detailsData: DetailsData[T],
    detail: T,
    f: Nino => ResponseValidation[T]): Assertion = {

    val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string
    val queryParams = detailQueryParams(detailsData.fields)

    implicit val format: OFormat[T] = detailsData.format
    stubCall(HttpMethod.Get, path, OK, Json.toJson(detail).toString, queryParams)

    val response = await(f(nino))

    verifyHeaders(HttpMethod.Get, path, queryParams)
    assert(response.fold(_ => false, _ => true))
  }

  private def verifyErrorResponse(nino: Nino, status: Int, baseError: BaseError): Assertion = {
    val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string
    val queryParams = detailQueryParams(IndividualDetail0.fields)
    stubCall(HttpMethod.Get, path, status, errorResponseFromIF(), queryParams)

    val response = await(connector.getDetail0(nino))

    verifyHeaders(HttpMethod.Get, path, queryParams)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
