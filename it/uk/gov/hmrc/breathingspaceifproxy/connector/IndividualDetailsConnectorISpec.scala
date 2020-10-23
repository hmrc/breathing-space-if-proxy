package uk.gov.hmrc.breathingspaceifproxy.connector

import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class IndividualDetailsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  val connector = inject[IndividualDetailsConnector]

  "getMinimalPopulation" should {
    "return a MinimalPopulation instance when it receives a 200(OK) response" in {
      val nino = genNino
      val url = urlWithoutQuery(IndividualDetailsConnector.path(nino, IndividualDetailsConnector.minimalPopulation))
      val responsePayload = Json.toJson(individualDetailsMinimalResponse(nino.value)).toString
      stubCall(HttpMethod.Get, url, Status.OK, responsePayload, minimalPopulation)

      val response = await(connector.getMinimalPopulation(nino))

      verifyHeaders(HttpMethod.Get, url, minimalPopulation.head)
      assert(response.fold(_ => false, _ => true))
    }

    "return RESOURCE_NOT_FOUND when the provided resource is unknown" in {
      val unknownNino = genNino
      val url = urlWithoutQuery(IndividualDetailsConnector.path(unknownNino, IndividualDetailsConnector.minimalPopulation))
      stubCall(HttpMethod.Get, url, Status.NOT_FOUND, errorResponsePayloadFromIF, minimalPopulation)

      val response = await(connector.getMinimalPopulation(unknownNino))

      verifyHeaders(HttpMethod.Get, url, minimalPopulation.head)
      response.fold(_.head.baseError shouldBe RESOURCE_NOT_FOUND, _ => notAnErrorInstance)
    }

    "return CONFLICTING_REQUEST in case of duplicated requests" in {
      val nino = genNino
      val url = urlWithoutQuery(IndividualDetailsConnector.path(nino, IndividualDetailsConnector.minimalPopulation))
      stubCall(HttpMethod.Get, url, Status.CONFLICT, errorResponsePayloadFromIF, minimalPopulation)

      val response = await(connector.getMinimalPopulation(nino))

      verifyHeaders(HttpMethod.Get, url, minimalPopulation.head)
      response.fold(_.head.baseError shouldBe CONFLICTING_REQUEST, _ => notAnErrorInstance)
    }

    "return SERVER_ERROR for any 4xx error, 404 and 409 excluded" in {
      val nino = genNino
      val url = urlWithoutQuery(IndividualDetailsConnector.path(nino, IndividualDetailsConnector.minimalPopulation))
      stubCall(HttpMethod.Get, url, Status.BAD_REQUEST, errorResponsePayloadFromIF, minimalPopulation)

      val response = await(connector.getMinimalPopulation(nino))

      verifyHeaders(HttpMethod.Get, url, minimalPopulation.head)
      response.fold(_.head.baseError shouldBe SERVER_ERROR, _ => notAnErrorInstance)
    }

    "return SERVER_ERROR for any 5xx error" in {
      val nino = genNino
      val url = urlWithoutQuery(IndividualDetailsConnector.path(nino, IndividualDetailsConnector.minimalPopulation))
      stubCall(HttpMethod.Get, url, Status.BAD_GATEWAY, errorResponsePayloadFromIF, minimalPopulation)

      val response = await(connector.getMinimalPopulation(nino))

      verifyHeaders(HttpMethod.Get, url, minimalPopulation.head)
      response.fold(_.head.baseError shouldBe SERVER_ERROR, _ => notAnErrorInstance)
    }
  }
}
