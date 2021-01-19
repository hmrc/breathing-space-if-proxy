package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.DebtsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.DebtsController.get
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Debts_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class DebtsControllerISpec extends BaseISpec {

  val nino = genNino
  val getPathWithValidNino = get(nino.value).url
  val debtsConnectorUrl = DebtsConnector.path(nino)

  "GET BS Debts for Nino" should {

    "return 200(OK) and all debts for the valid Nino provided" in {
      verifyOk(attended = true)
    }

    "return 200(OK) even for a valid Nino with a trailing blank" in {
      val ninoWithoutSuffix = genNino
      val controllerUrl = get(s"${ninoWithoutSuffix.value} ").url
      val connectorUrl =
        DebtsConnector
          .path(ninoWithoutSuffix)
          .replace(ninoWithoutSuffix.value, s"${ninoWithoutSuffix.value}%20")

      stubCall(HttpMethod.Get, connectorUrl, Status.OK, debtsAsSentFromEis)

      val response = route(app, fakeRequest(Helpers.GET, controllerUrl)).get

      status(response) shouldBe Status.OK
      contentAsString(response) shouldBe debts

      verifyHeaders(HttpMethod.Get, connectorUrl)
      verifyAuditEventCall(BS_Debts_GET)
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

    "return 401(UNAUTHORIZED) when the request was not authorised" in {
      verifyUnauthorized(fakeRequest(Helpers.GET, getPathWithValidNino))
    }

    "return 403(BREATHING_SPACE_EXPIRED) when Breathing Space has expired for the given Nino" in {
      stubCall(HttpMethod.Get, debtsConnectorUrl, Status.FORBIDDEN, errorResponseFromIF())

      val response = await(route(app, fakeRequest(Helpers.GET, getPathWithValidNino)).get)
      val errorList = verifyErrorResult(response, FORBIDDEN, correlationIdAsString.some, 1)

      And(s"the error code should be $BREATHING_SPACE_EXPIRED")
      errorList.head.code shouldBe BREATHING_SPACE_EXPIRED.entryName
      assert(errorList.head.message.startsWith(BREATHING_SPACE_EXPIRED.message))
    }

    "return 404(RESOURCE_NOT_FOUND) when the provided Nino is unknown" in {
      val unknownNino = genNino
      val url = DebtsConnector.path(unknownNino)
      stubCall(HttpMethod.Get, url, Status.NOT_FOUND, errorResponseFromIF())
      val response = route(app, fakeRequest(Helpers.GET, get(unknownNino.value).url)).get
      status(response) shouldBe Status.NOT_FOUND

      verifyHeaders(HttpMethod.Get, url)
      verifyAuditEventCall(BS_Debts_GET)
    }

    "return 404(NO_DATA_FOUND) when no records were found for the provided Nino" in {
      stubCall(HttpMethod.Get, debtsConnectorUrl, Status.NOT_FOUND, errorResponseFromIF("NO_DATA_FOUND"))

      val response = await(route(app, fakeRequest(Helpers.GET, getPathWithValidNino)).get)
      val errorList = verifyErrorResult(response, NOT_FOUND, correlationIdAsString.some, 1)

      And(s"the error code should be $NO_DATA_FOUND")
      errorList.head.code shouldBe NO_DATA_FOUND.entryName
      assert(errorList.head.message.startsWith(NO_DATA_FOUND.message))
    }

    "return 404(NOT_IN_BREATHING_SPACE) when the given Nino is not in Breathing Space" in {
      stubCall(HttpMethod.Get, debtsConnectorUrl, Status.NOT_FOUND, errorResponseFromIF("IDENTIFIER_NOT_IN_BREATHINGSPACE"))

      val response = await(route(app, fakeRequest(Helpers.GET, getPathWithValidNino)).get)
      val errorList = verifyErrorResult(response, NOT_FOUND, correlationIdAsString.some, 1)

      And(s"the error code should be $NOT_IN_BREATHING_SPACE")
      errorList.head.code shouldBe NOT_IN_BREATHING_SPACE.entryName
      assert(errorList.head.message.startsWith(NOT_IN_BREATHING_SPACE.message))
    }
  }

  private def verifyOk(attended: Boolean): Assertion = {
    stubCall(HttpMethod.Get, debtsConnectorUrl, Status.OK, debtsAsSentFromEis)

    val request =
      if (attended) fakeRequest(Helpers.GET, getPathWithValidNino)
      else fakeRequestForUnattended(Helpers.GET, getPathWithValidNino)

    val response = route(app, request).get
    status(response) shouldBe Status.OK
    contentAsString(response) shouldBe debts

    if (attended) verifyHeadersForAttended(HttpMethod.Get, debtsConnectorUrl)
    else verifyHeadersForUnattended(HttpMethod.Get, debtsConnectorUrl)

    verifyAuditEventCall(BS_Debts_GET)
  }
}
