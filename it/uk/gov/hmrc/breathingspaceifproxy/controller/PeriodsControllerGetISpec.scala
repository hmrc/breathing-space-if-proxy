package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.syntax.option._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.PeriodsController.get
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.INVALID_BODY
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsControllerGetISpec extends BaseISpec {

  val getPathWithValidNino = get(validNinoAsString).url
  val getPathWithUnknownNino = get(unknownNino.value).url

  "GET BS Periods for Nino" should {

    "return 200(OK) and all periods for the valid Nino provided" in {
      val expectedBody = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Get, periodsConnectorUrl, Status.OK, expectedBody)

      val response = route(app, fakeRequest(Helpers.GET, getPathWithValidNino)).get
      status(response) shouldBe Status.OK

      verifyHeadersForAttended(HttpMethod.Get, periodsConnectorUrl)
      contentAsString(response) shouldBe expectedBody
    }

    "return 200(OK) and an empty list of periods for the valid Nino provided" in {
      val expectedBody = """{"periods":[]}"""
      stubCall(HttpMethod.Get, periodsConnectorUrl, Status.OK, expectedBody)

      val response = route(app, fakeRequest(Helpers.GET, getPathWithValidNino)).get
      status(response) shouldBe Status.OK

      verifyHeadersForAttended(HttpMethod.Get, periodsConnectorUrl)
      contentAsString(response) shouldBe expectedBody
    }

    "return 200(OK) for an unattended request with a valid Nino" in {
      val expectedBody = """{"periods":[]}"""
      stubCall(HttpMethod.Get, periodsConnectorUrl, Status.OK, expectedBody)

      val response = route(app, fakeRequestForUnattended(Helpers.GET, getPathWithValidNino)).get
      status(response) shouldBe Status.OK

      verifyHeadersForUnattended(HttpMethod.Get, periodsConnectorUrl)
      contentAsString(response) shouldBe expectedBody
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
      val url = PeriodsConnector.path(unknownNino)
      stubCall(HttpMethod.Get, url, Status.NOT_FOUND, errorResponsePayloadFromIF)
      val response = route(app, fakeRequest(Helpers.GET, getPathWithUnknownNino)).get
      status(response) shouldBe Status.NOT_FOUND
      verifyHeadersForAttended(HttpMethod.Get, url)
    }
  }
}
