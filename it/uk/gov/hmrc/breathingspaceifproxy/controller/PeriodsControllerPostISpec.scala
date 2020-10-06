package uk.gov.hmrc.breathingspaceifproxy.controller

import cats.syntax.option._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.breathingspaceifproxy.connector.PeriodsConnector
import uk.gov.hmrc.breathingspaceifproxy.controller.routes.PeriodsController.post
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsControllerPostISpec extends BaseISpec {

  val postPath = post.url

  "POST BS Periods for Nino" should {

    "return 201(CREATED) and all periods for the valid Nino provided" in {
      val expectedBody = Json.toJson(validPeriodsResponse).toString
      stubCall(HttpMethod.Post, periodsConnectorUrl, Status.CREATED, expectedBody)

      val body = postPeriodsRequestAsJson(postPeriodsRequest)
      val request = fakeRequest(Helpers.POST, postPath).withBody(body)

      val response = route(app, request).get
      status(response) shouldBe Status.CREATED

      verifyHeadersForAttended(HttpMethod.Post, periodsConnectorUrl)
      contentAsString(response) shouldBe expectedBody
    }

    "return 400(BAD_REQUEST) when no body is provided" in {
      val request = fakeRequest(Helpers.POST, postPath)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $MISSING_BODY")
      errorList.head.code shouldBe MISSING_BODY.entryName
      assert(errorList.head.message.startsWith(MISSING_BODY.message))
    }

    "return 400(BAD_REQUEST) when body is not valid Json" in {
      val body = s"""{nino":"$nino","periods":[${Json.toJson(validPostPeriod).toString}]}"""
      val request = fakeRequest(Helpers.POST, postPath).withBody(body)

      val response = await(route(app, request).get)

      val errorList = verifyErrorResult(response, BAD_REQUEST, correlationIdAsString.some, 1)

      And(s"the error code should be $INVALID_JSON")
      errorList.head.code shouldBe INVALID_JSON.entryName
      assert(errorList.head.message.startsWith(INVALID_JSON.message))
    }

    "return 404(NOT_FOUND) when the provided Nino is unknown" in {
      val url = PeriodsConnector.path(unknownNino)
      stubCall(HttpMethod.Post, url, Status.NOT_FOUND, errorResponsePayloadFromIF)

      val request = fakeRequest(Helpers.POST, postPath)
        .withBody(postPeriodsRequestAsJson(unknownNino.value, postPeriodsRequest))

      val response = route(app, request).get
      status(response) shouldBe Status.NOT_FOUND

      verifyHeadersForAttended(HttpMethod.Post, url)
    }
  }
}
