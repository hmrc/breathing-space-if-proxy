package uk.gov.hmrc.breathingspaceifproxy.connector

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status._
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Debts_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class DebtsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  val connector = inject[DebtsConnector]
  implicit val requestId = genRequestId(BS_Debts_GET, connector.etmpConnector)

  "get" should {
    "return a Debts instance when it receives a 200(OK) response" in {
      val nino = genNino
      val url = DebtsConnector.path(nino)
      stubCall(HttpMethod.Get, url, OK, debtsAsSentFromEis)

      val response = await(connector.get(nino))

      verifyHeaders(HttpMethod.Get, url)
      assert(response.fold(_ => false, _ => true))
    }

    "return BREATHING_SPACE_EXPIRED when Breathing Space has expired for the given Nino" in {
      verifyGetResponse(FORBIDDEN, BREATHING_SPACE_EXPIRED, "BREATHINGSPACE_EXPIRED".some)
    }

    "return RESOURCE_NOT_FOUND when the given Nino is unknown" in {
      verifyGetResponse(NOT_FOUND, RESOURCE_NOT_FOUND, "IDENTIFIER_NOT_FOUND".some)
    }

    "return NO_DATA_FOUND when the given Nino has no debts" in {
      verifyGetResponse(NOT_FOUND, NO_DATA_FOUND, "NO_DATA_FOUND".some)
    }

    "return NOT_IN_BREATHING_SPACE when the given Nino is not in Breathing Space" in {
      verifyGetResponse(NOT_FOUND, NOT_IN_BREATHING_SPACE, "IDENTIFIER_NOT_IN_BREATHINGSPACE".some)
    }

    "return CONFLICTING_REQUEST in case of duplicated requests" in {
      verifyGetResponse(CONFLICT, CONFLICTING_REQUEST, "CONFLICTING_REQUEST".some)
    }

    "return SERVER_ERROR for any 4xx error, 403, 404 and 409 excluded" in {
      verifyGetResponse(BAD_REQUEST, SERVER_ERROR)
    }

    "return SERVER_ERROR for any 5xx error, 502, 503 and 504 excluded" in {
      verifyGetResponse(NOT_IMPLEMENTED, SERVER_ERROR)
    }
  }

  private def verifyGetResponse(status: Int, baseError: BaseError, code: Option[String] = none): Assertion = {
    val nino = genNino
    val url = DebtsConnector.path(nino)
    stubCall(HttpMethod.Get, url, status, errorResponseFromIF(code.fold(baseError.entryName)(identity)))

    val response = await(connector.get(nino))

    verifyHeaders(HttpMethod.Get, url)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
