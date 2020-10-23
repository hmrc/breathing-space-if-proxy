package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.PeriodsController.get
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.INVALID_BODY
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsControllerGetISpec extends BaseISpec {

  val nino = genNino
  val getPathWithValidNino = get(nino.value).url
  val periodsConnectorUrl = PeriodsConnector.path(nino)

  "GET BS Periods for Nino" should {

    "return 200(OK) and all periods for the valid Nino provided" in {
      verifyOk(attended = true)
    }

    "return 200(OK) even for a valid Nino with a trailing blank" in {
      val ninoWithoutSuffix = genNino
      val controllerUrl = get(s"${ninoWithoutSuffix.value} ").url
      val connectorUrl =
        PeriodsConnector
          .path(ninoWithoutSuffix)
          .replace(ninoWithoutSuffix.value, s"${ninoWithoutSuffix.value}%20")

      val expectedBody = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Get, connectorUrl, Status.OK, expectedBody)

      val response = route(app, fakeRequest(Helpers.GET, controllerUrl)).get

      status(response) shouldBe Status.OK
      verifyHeaders(HttpMethod.Get, connectorUrl)
      contentAsString(response) shouldBe expectedBody
    }

    "return 200(OK) and an empty list of periods for the valid Nino provided" in {
      val expectedBody = """{"periods":[]}"""
      stubCall(HttpMethod.Get, periodsConnectorUrl, Status.OK, expectedBody)

      val response = route(app, fakeRequest(Helpers.GET, getPathWithValidNino)).get
      status(response) shouldBe Status.OK

      verifyHeaders(HttpMethod.Get, periodsConnectorUrl)
      contentAsString(response) shouldBe expectedBody
    }

    "return 200(OK) for an ATTENDED request" in {
      verifyOk(attended = true)
    }

    "return 200(OK) for an UNATTENDED request" in {
      verifyOk(attended = false)
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
      val url = PeriodsConnector.path(unknownNino)
      stubCall(HttpMethod.Get, url, Status.NOT_FOUND, errorResponsePayloadFromIF)
      val response = route(app, fakeRequest(Helpers.GET, get(unknownNino.value).url)).get
      status(response) shouldBe Status.NOT_FOUND
      verifyHeaders(HttpMethod.Get, url)
    }
  }

  private def verifyOk(attended: Boolean): Assertion = {
    val expectedResponseBody = Json.toJson(validPeriodsResponse).toString
    stubCall(HttpMethod.Get, periodsConnectorUrl, Status.OK, expectedResponseBody)

    val request =
      if (attended) fakeRequest(Helpers.GET, getPathWithValidNino)
      else fakeRequestForUnattended(Helpers.GET, getPathWithValidNino)

    val response = route(app, request).get
    status(response) shouldBe Status.OK

    if (attended) verifyHeadersForAttended(HttpMethod.Get, periodsConnectorUrl)
    else verifyHeadersForUnattended(HttpMethod.Get, periodsConnectorUrl)

    contentAsString(response) shouldBe expectedResponseBody
  }
}
