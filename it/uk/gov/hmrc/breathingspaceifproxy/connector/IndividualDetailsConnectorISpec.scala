package uk.gov.hmrc.breathingspaceifproxy.connector

import org.scalatest.Assertion
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Details_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class IndividualDetailsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  implicit val requestId = genRequestId(BS_Details_GET)
  val connector = inject[IndividualDetailsConnector]

  "get" should {
    "return a Detail0 instance when it receives the relative \"fields\" query parameter" in {
      val nino = genNino
      val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string
      val queryParams = detailQueryParams(IndividualDetails.fields)

      stubCall(HttpMethod.Get, path, OK, Json.toJson(details(nino)).toString, queryParams)

      val response = await(connector.getDetails(nino))

      verifyHeaders(HttpMethod.Get, path, queryParams)
      assert(response.fold(_ => false, _ => true))
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
      val queryParamsForDetail0 = detailQueryParams(IndividualDetails.fields)

      stubCall(HttpMethod.Get, path, OK, unexpectedPayload, queryParamsForDetail0)

      val response = await(connector.getDetails(nino))

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

  private def verifyErrorResponse(nino: Nino, status: Int, baseError: BaseError): Assertion = {
    val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string
    val queryParams = detailQueryParams(IndividualDetails.fields)
    stubCall(HttpMethod.Get, path, status, errorResponseFromIF(), queryParams)

    val response = await(connector.getDetails(nino))

    verifyHeaders(HttpMethod.Get, path, queryParams)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
