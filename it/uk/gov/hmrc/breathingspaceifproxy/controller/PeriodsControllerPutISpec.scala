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

  val putPath = put(genNinoString).url

  "PUT BS Periods for Nino" should {

    "return 200(OK) and all periods for the valid Nino provided" in {
      verifyOk(attended = true)
    }

    "return 200(OK) even for a valid Nino with a trailing blank" in {
      val ninoWithoutSuffix = genNino
      val controllerUrl = put(s"${ninoWithoutSuffix.value} ").url
      val connectorUrl =
        PeriodsConnector
          .path(ninoWithoutSuffix)
          .replace(ninoWithoutSuffix.value, s"${ninoWithoutSuffix.value}%20")

      val expectedBody = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Put, connectorUrl, Status.OK, expectedBody)

      val request = fakeRequest(Helpers.PUT, controllerUrl).withBody(putPeriodsRequestAsJson(putPeriodsRequest))
      val response = route(app, request).get

      status(response) shouldBe Status.OK
      verifyHeaders(HttpMethod.Put, connectorUrl)
      contentAsString(response) shouldBe expectedBody
    }

    "return 200(OK) for an ATTENDED request" in {
      verifyOk(attended = true)
    }

    "return 200(OK) for an UNATTENDED request" in {
      verifyOk(attended = false)
    }

    "return 400(BAD_REQUEST) when no body is provided" in {
      val request = fakeRequest(Helpers.PUT, putPath)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_BODY")
      errorList.head.code shouldBe MISSING_BODY.entryName
      assert(errorList.head.message.startsWith(MISSING_BODY.message))
    }

    "return 400(BAD_REQUEST) when body is not valid Json" in {
      val body = s"""{periods":[${Json.toJson(validPutPeriod).toString}]}"""
      val request = fakeRequest(Helpers.PUT, putPath).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_JSON")
      errorList.head.code shouldBe INVALID_JSON.entryName
      assert(errorList.head.message.startsWith(INVALID_JSON.message))
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val unknownNino = genNino
      val connectorUrl = PeriodsConnector.path(unknownNino)
      stubCall(HttpMethod.Put, connectorUrl, Status.NOT_FOUND, errorResponsePayloadFromIF)

      val controllerUrl = put(unknownNino.value).url
      val request = fakeRequest(Helpers.PUT, controllerUrl)
        .withBody(putPeriodsRequestAsJson(putPeriodsRequest))

      val response = route(app, request).get

      status(response) shouldBe Status.NOT_FOUND
      verifyHeaders(HttpMethod.Put, connectorUrl)
    }
  }

  private def verifyOk(attended: Boolean): Assertion = {
    val nino = genNino
    val connectorUrl = PeriodsConnector.path(nino)
    val expectedBody = Json.toJson(validPeriodsResponse).toString
    stubCall(HttpMethod.Put, connectorUrl, Status.OK, expectedBody)

    val controllerUrl = put(nino.value).url

    val request =
      (if (attended) fakeRequest(Helpers.PUT, controllerUrl)
      else fakeRequestForUnattended(Helpers.PUT, controllerUrl))
        .withBody(putPeriodsRequestAsJson(putPeriodsRequest))

    val response = route(app, request).get
    status(response) shouldBe Status.OK

    if (attended) verifyHeadersForAttended(HttpMethod.Put, connectorUrl)
    else verifyHeadersForUnattended(HttpMethod.Put, connectorUrl)

    contentAsString(response) shouldBe expectedBody
  }
}
