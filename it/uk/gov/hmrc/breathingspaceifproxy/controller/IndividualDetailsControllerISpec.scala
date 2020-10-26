package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.syntax.option._
import org.scalatest.Assertion
import play.api.http.Status
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.IndividualDetailsController.get
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod, TestingErrorItem}

class IndividualDetailsControllerISpec extends BaseISpec {

  val nino = genNino

  "GET Individual's details for the provided Nino" should {

    "return 200(OK) and the expected individual details when detailId is equal to 0" in {
      verifyResponse(attended = true, nino, DetailData0, detail0(nino), 0)
    }

    "return 200(OK) and the expected individual details when detailId is equal to 1" in {
      verifyResponse(attended = true, nino, DetailData1, detail1(nino), 1)
    }

    "return 200(OK) for an ATTENDED request" in {
      verifyResponse(attended = true, nino, DetailData0, detail0(nino), 0)
    }

    "return 200(OK) for an UNATTENDED request" in {
      verifyResponse(attended = false, nino, DetailData0, detail0(nino), 0)
    }

    "return 400(BAD_REQUEST) when a body is provided" in {
      val body = Json.obj("aName" -> "aValue")
      val request = fakeRequest(Helpers.GET, get(nino.value, 0).url).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_BODY")
      errorList.head.code shouldBe INVALID_BODY.entryName
      assert(errorList.head.message.startsWith(INVALID_BODY.message))
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      verifyResponse(attended = true, nino, DetailData0, detail0(nino), 0, RESOURCE_NOT_FOUND.some)
    }

    "return 409(CONFLICT) in case of duplicated requests" in {
      verifyResponse(attended = true, nino, DetailData0, detail0(nino), 0, CONFLICTING_REQUEST.some)
    }
  }

  private def verifyResponse[T <: Detail](
    attended: Boolean, nino: Nino, detailData: DetailData[T], detail: T, detailId: Int, error: Option[BaseError] = none
  ): Assertion = {
    implicit val format: OFormat[T] = detailData.format

    val connectorUrl = urlWithoutQuery(IndividualDetailsConnector.path(nino, detailData.fields))

    val expectedStatus = error.fold(Status.OK)(_.httpCode)
    val expectedResponseBody =
      if (expectedStatus == Status.OK) Json.toJson(detail).toString
      else Json.obj("errors" -> List(TestingErrorItem(error.get.entryName, error.get.message))).toString

    val queryParams = detailQueryParams(detailData.fields)

    stubCall(HttpMethod.Get, connectorUrl, new Integer(expectedStatus), expectedResponseBody, queryParams)

    val path = get(nino.value, detailId).url

    val request =
      if (attended) fakeRequest(Helpers.GET, path)
      else fakeRequestForUnattended(Helpers.GET, path)

    val response = route(app, request).get
    status(response) shouldBe expectedStatus

    if (attended) verifyHeadersForAttended(HttpMethod.Get, connectorUrl, queryParams.head)
    else verifyHeadersForUnattended(HttpMethod.Get, connectorUrl, queryParams.head)

    contentAsString(response) shouldBe expectedResponseBody
  }
}
