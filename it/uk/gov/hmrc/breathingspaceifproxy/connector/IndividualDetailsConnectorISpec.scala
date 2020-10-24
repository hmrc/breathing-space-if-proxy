package uk.gov.hmrc.breathingspaceifproxy.connector

import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class IndividualDetailsConnectorISpec extends BaseISpec with ConnectorTestSupport {

  val connector = inject[IndividualDetailsConnector]

  "get" should {
    "return a Detail0 instance when it receives the relative \"fields\" query parameter" in {
      val nino = genNino
      verifyResponse(nino, DetailData0, detail0(nino))
    }

    "return a Detail1 instance when it receives the relative \"fields\" query parameter" in {
      val nino = genNino
      verifyResponse(nino, DetailData1, detail1(nino))
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

      implicit val formatForDetail1: OFormat[Detail1] = DetailData1.format
      val unexpectedPayload = Json.toJson(detail1(nino)).toString

      stubCall(HttpMethod.Get, urlForDetail0, Status.OK, unexpectedPayload, queryParamsForDetail0)

      val response = await(connector.get[Detail0](nino, DetailData0))

      verifyHeaders(HttpMethod.Get, urlForDetail0, queryParamsForDetail0.head)
      response.fold(_.head.baseError shouldBe SERVER_ERROR, _ => notAnErrorInstance)
    }

    "return SERVER_ERROR for any 4xx error, 404 and 409 excluded" in {
      verifyErrorResponse(genNino, Status.BAD_REQUEST, SERVER_ERROR)
    }

    "return SERVER_ERROR for any 5xx error" in {
      verifyErrorResponse(genNino, Status.BAD_GATEWAY, SERVER_ERROR)
    }
  }

  private def verifyResponse[T <: Detail](nino: Nino, detailData: DetailData[T], detail: T): Assertion = {
    implicit val format: OFormat[T] = detailData.format

    val url = urlWithoutQuery(IndividualDetailsConnector.path(nino, detailData.fields))
    val queryParams = detailQueryParams(detailData.fields)

    stubCall(HttpMethod.Get, url, Status.OK, Json.toJson(detail).toString, queryParams)

    val response = await(connector.get[T](nino, detailData))

    verifyHeaders(HttpMethod.Get, url, queryParams.head)
    assert(response.fold(_ => false, _ => true))
  }

  private def verifyErrorResponse(nino: Nino, status: Int, baseError: BaseError): Assertion = {
    val url = urlWithoutQuery(IndividualDetailsConnector.path(nino, DetailData0.fields))
    val queryParams = detailQueryParams(DetailData0.fields)
    stubCall(HttpMethod.Get, url, status, errorResponsePayloadFromIF, queryParams)

    val response = await(connector.get[Detail0](nino, DetailData0))

    verifyHeaders(HttpMethod.Get, url, queryParams.head)
    response.fold(_.head.baseError shouldBe baseError, _ => notAnErrorInstance)
  }
}
