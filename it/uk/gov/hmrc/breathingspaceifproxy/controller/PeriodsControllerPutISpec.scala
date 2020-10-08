package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.PeriodsController.put
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.{INVALID_JSON, MISSING_BODY}
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsControllerPutISpec extends BaseISpec {

  val putPathWithValidNino = put(validNinoAsString).url
  val putPathWithUnknownNino = put(unknownNino.value).url

  "PUT BS Periods for Nino" should {

    "return 200(OK) and all periods for the valid Nino provided" in {
      verifyOk(attended = true)
    }

    "return 200(OK) for an ATTENDED request" in {
      verifyOk(attended = true)
    }

    "return 200(OK) for an UNATTENDED request" in {
      verifyOk(attended = false)
    }

    "return 400(BAD_REQUEST) when no body is provided" in {
      val request = fakeRequest(Helpers.PUT, putPathWithValidNino)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_BODY")
      errorList.head.code shouldBe MISSING_BODY.entryName
      assert(errorList.head.message.startsWith(MISSING_BODY.message))
    }

    "return 400(BAD_REQUEST) when body is not valid Json" in {
      val body = s"""{periods":[${Json.toJson(validPostPeriod).toString}]}"""
      val request = fakeRequest(Helpers.PUT, putPathWithValidNino).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_JSON")
      errorList.head.code shouldBe INVALID_JSON.entryName
      assert(errorList.head.message.startsWith(INVALID_JSON.message))
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val url = PeriodsConnector.path(unknownNino)
      stubCall(HttpMethod.Put, url, Status.NOT_FOUND, errorResponsePayloadFromIF)

      val request = fakeRequest(Helpers.PUT, putPathWithUnknownNino)
        .withBody(putPeriodsRequestAsJson(putPeriodsRequest))

      val response = route(app, request).get
      status(response) shouldBe Status.NOT_FOUND

      verifyHeaders(HttpMethod.Put, url)
    }
  }

  private def verifyOk(attended: Boolean): Assertion = {
    val expectedBody = Json.toJson(validPeriodsResponse).toString
    stubCall(HttpMethod.Put, periodsConnectorUrl, Status.OK, expectedBody)

    val request =
      (if (attended) fakeRequest(Helpers.PUT, putPathWithValidNino)
      else fakeRequestForUnattended(Helpers.PUT, putPathWithValidNino))
        .withBody(putPeriodsRequestAsJson(putPeriodsRequest))

    val response = route(app, request).get
    status(response) shouldBe Status.OK

    if (attended) verifyHeadersForAttended(HttpMethod.Put, periodsConnectorUrl)
    else verifyHeadersForUnattended(HttpMethod.Put, periodsConnectorUrl)

    contentAsString(response) shouldBe expectedBody
  }
}
