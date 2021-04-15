package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.IndividualDetailsController.getDetails
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.model.enums.EndpointId.BS_Details_GET
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod, TestingErrorItem}

class IndividualDetailsControllerISpec extends BaseISpec {

  "GET Individual's details for the provided Nino" should {

    "return 200(OK) and the expected individual details, according to the expected filter" in {
      verifyResponse(attended = true)
    }

    "return 200(OK) for an ATTENDED request" in {
      verifyResponse(attended = true)
    }

    "return 200(OK) for an UNATTENDED request" in {
      verifyResponse(attended = false)
    }

    "return 400(BAD_REQUEST) when a body is provided" in {
      val body = Json.obj("aName" -> "aValue")
      val request = fakeAttendedRequest(Helpers.GET, getDetails(genNino.value).url).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_BODY")
      errorList.head.code shouldBe INVALID_BODY.entryName
      assert(errorList.head.message.startsWith(INVALID_BODY.message))
    }

    "return 401(UNAUTHORIZED) when the request was not authorised" in {
      verifyUnauthorized(fakeAttendedRequest(Helpers.GET, getDetails(genNino.value).url))
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      verifyResponse(attended = true, RESOURCE_NOT_FOUND.some)
    }

    "return 409(CONFLICT) in case of duplicated requests" in {
      verifyResponse(attended = true, CONFLICTING_REQUEST.some)
    }
  }

  private def verifyResponse(attended: Boolean, error: Option[BaseError] = none): Assertion = {
    val nino = genNino
    val path = IndividualDetailsConnector.path(nino, "")  // queryParams here must be an empty string

    val expectedStatus = error.fold(Status.OK)(_.httpCode)
    val expectedResponseBody =
      if (expectedStatus == Status.OK) Json.toJson(details(nino)).toString
      else Json.obj("errors" -> List(TestingErrorItem(error.get.entryName, error.get.message))).toString

    val queryParams = detailQueryParams(IndividualDetails.fields)

    stubCall(HttpMethod.Get, path, expectedStatus, expectedResponseBody, queryParams)

    val request =
      if (attended) fakeAttendedRequest(Helpers.GET, getDetails(nino.value).url)
      else fakeUnattendedRequest(Helpers.GET, getDetails(nino.value).url)

    val response = route(app, request).get
    status(response) shouldBe expectedStatus
    contentAsString(response) shouldBe expectedResponseBody

    if (attended) verifyHeadersForAttended(HttpMethod.Get, path, queryParams)
    else verifyHeadersForUnattended(HttpMethod.Get, path, queryParams)

    verifyAuditEventCall(BS_Details_GET)
  }
}
