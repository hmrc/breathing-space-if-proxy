package uk.gov.hmrc.breathingspaceifproxy.controller

import java.time.{LocalDate, ZonedDateTime}

import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsControllerISpec extends BaseISpec {

  val getPathWithValidNino = s"/$validNinoAsString/periods"
  val postPath = "/periods"

  "GET /:nino/periods" should {

    "return 200(OK) and all periods for the valid Nino provided" in {
      val url = PeriodsConnector.path(nino)
      val expectedBody = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Get, url, Status.OK, expectedBody)

      val response = route(app, fakeRequest(Helpers.GET, getPathWithValidNino)).get
      status(response) shouldBe Status.OK

      verifyHeadersForGet(url)
      contentAsString(response) shouldBe expectedBody
    }

    "return 200(OK) and an empty list of periods for the valid Nino provided" in {
      val url = PeriodsConnector.path(nino)
      val expectedBody = """{"periods":[]}"""
      stubCall(HttpMethod.Get, url, Status.OK, expectedBody)

      val response = route(app, fakeRequest(Helpers.GET, getPathWithValidNino)).get
      status(response) shouldBe Status.OK

      verifyHeadersForGet(url)
      contentAsString(response) shouldBe expectedBody
    }

    "return 200(OK) for an unattended request with a valid Nino" in {
      val url = PeriodsConnector.path(nino)
      val expectedBody = """{"periods":[]}"""
      stubCall(HttpMethod.Get, url, Status.OK, expectedBody)

      val response = route(app, fakeRequestForUnattended(Helpers.GET, getPathWithValidNino)).get
      status(response) shouldBe Status.OK

      verifyHeadersForGetUnattended(url)
      contentAsString(response) shouldBe expectedBody
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val url = PeriodsConnector.path(unknownNino)
      stubCall(HttpMethod.Get, url, Status.NOT_FOUND, errorResponsePayloadFromIF)
      val response = route(app, fakeRequest(Helpers.GET, s"/${unknownNino.value}/periods")).get
      status(response) shouldBe Status.NOT_FOUND
      verifyHeadersForGet(url)
    }
  }

  "POST /periods" should {

    "return 201(CREATED) and all periods for the valid Nino provided" in {
      val url = PeriodsConnector.path(nino)
      val expectedBody = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Post, url, Status.CREATED, expectedBody)

      val request = fakeRequest(Helpers.POST, postPath)
        .withJsonBody(createPeriodsRequest(validPeriods))

      val response = route(app, request).get
      status(response) shouldBe Status.CREATED

      verifyHeadersForPost(url)
      contentAsString(response) shouldBe expectedBody
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val url = PeriodsConnector.path(unknownNino)
      stubCall(HttpMethod.Post, url, Status.NOT_FOUND, errorResponsePayloadFromIF)

      val cpr = createPeriodRequestAsJson(
        unknownNino.value,
        LocalDate.now.minusDays(1).toString, LocalDate.now.toString,
        ZonedDateTime.now.toString
      )

      val response = route(app, fakeRequest(Helpers.POST, postPath).withJsonBody(cpr)).get
      status(response) shouldBe Status.NOT_FOUND

      verifyHeadersForPost(url)
    }
  }
}
