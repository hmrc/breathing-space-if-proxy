package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.IndividualDetailsController.getMinimalPopulation
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.INVALID_BODY
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class IndividualDetailsControllerISpec extends BaseISpec {

  val nino = genNino
  val getPathWithValidNino = getMinimalPopulation(nino.value).url
  val connector = inject[IndividualDetailsConnector]
  val connectorUrl = urlWithoutQuery(IndividualDetailsConnector.path(nino, IndividualDetailsConnector.minimalPopulation))

  "GET Individual's minimal details for the provided Nino" should {

    "return 200(OK) and the individual's minimal details for the valid Nino provided" in {
      verifyOk(attended = true, genNinoString)
    }

    "return 200(OK) for an ATTENDED request" in {
      verifyOk(attended = true, genNinoString)
    }

    "return 200(OK) for an UNATTENDED request" in {
      verifyOk(attended = false, genNinoString)
    }

    "return 400(BAD_REQUEST) when a body is provided" in {
      val body = Json.obj("aName" -> "aValue")
      val request = fakeRequest(Helpers.GET, getPathWithValidNino).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_BODY")
      errorList.head.code shouldBe INVALID_BODY.entryName
      assert(errorList.head.message.startsWith(INVALID_BODY.message))
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val unknownNino = genNino
      val url = urlWithoutQuery(IndividualDetailsConnector.path(unknownNino, IndividualDetailsConnector.minimalPopulation))
      stubCall(HttpMethod.Get, url, Status.NOT_FOUND, errorResponsePayloadFromIF, minimalPopulation)
      val response = route(app, fakeRequest(Helpers.GET, getMinimalPopulation(unknownNino.value).url)).get
      status(response) shouldBe Status.NOT_FOUND
      verifyHeaders(HttpMethod.Get, url, minimalPopulation.head)
    }
  }

  private def verifyOk(attended: Boolean, nino: String): Assertion = {
    val expectedResponseBody = Json.toJson(individualDetailsMinimalResponse(nino)).toString
    stubCall(HttpMethod.Get, connectorUrl, Status.OK, expectedResponseBody, minimalPopulation)

    val request =
      if (attended) fakeRequest(Helpers.GET, getPathWithValidNino)
      else fakeRequestForUnattended(Helpers.GET, getPathWithValidNino)

    val response = route(app, request).get
    status(response) shouldBe Status.OK

    if (attended) verifyHeadersForAttended(HttpMethod.Get, connectorUrl, minimalPopulation.head)
    else verifyHeadersForUnattended(HttpMethod.Get, connectorUrl, minimalPopulation.head)

    contentAsString(response) shouldBe expectedResponseBody
  }
}
