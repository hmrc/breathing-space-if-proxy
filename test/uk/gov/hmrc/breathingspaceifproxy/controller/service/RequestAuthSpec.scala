/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.breathingspaceifproxy.controller.service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import play.api.mvc.{AnyContent, Results}
import play.api.test.{Helpers => PlayHelpers}
import play.api.test.Helpers.status
import uk.gov.hmrc.auth.core.UnsupportedAuthProvider
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec.retrievalsTestingSyntax
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

class RequestAuthSpec extends AnyWordSpec with BaseSpec with RequestAuth with Results {

  val controllerComponents = PlayHelpers.stubControllerComponents()

  "authAction" should {

    "return 200(OK) when the request is authorized" in {
      val result = authAction("Some scope").invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe OK
    }

    "return 200(OK) when the nino in the request match the authenticated nino" in {
      val authResult: AuthRetrieval = Some("AA000000A") ~ None ~ None
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(authResult))

      val result =
        authAction("Some scope", Some("AA000000A")).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe OK
    }

    "return 200(OK) when the nino in the request match the trusted helper principal nino" in {
      val authResult: AuthRetrieval = Some("BB000000B") ~ Some(TrustedHelper("", "", "", "AA000000A")) ~ Some(
        "client-id"
      )
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(authResult))

      val result =
        authAction("Some scope", Some("AA000000A")).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe OK
    }

    "return 200(OK) when a nino is specified and a client id is present" in {
      val authResult: AuthRetrieval = None ~ None ~ Some("client-id")
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(authResult))

      val result =
        authAction("Some scope", Some("AA000000A")).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe OK
    }

    "return 401(UNAUTHORIZED) when the nino in the request does not match the authenticated nino" in {
      val authResult: AuthRetrieval = Some("AA000000A") ~ None ~ None
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(authResult))

      val result =
        authAction("Some scope", Some("AB000000A")).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe UNAUTHORIZED
    }

    "return 401(UNAUTHORIZED) when the nino in the request does not match the trusted helper principal nino" in {
      val authResult: AuthRetrieval = None ~ Some(TrustedHelper("", "", "", "AA000000A")) ~ None
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(authResult))

      val result =
        authAction("Some scope", Some("AB000000A")).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe UNAUTHORIZED
    }

    "return 401(UNAUTHORIZED) when a client Id is specified and the nino in the request does not match the authenticated nino" in {
      val authResult: AuthRetrieval = Some("AA000000A") ~ None ~ Some("client-id")
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(authResult))

      val result =
        authAction("Some scope", Some("AB000000A")).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe UNAUTHORIZED
    }

    "return 401(UNAUTHORIZED) when a client Id is specified the nino in the request does not match the trusted helper principal nino" in {
      val authResult: AuthRetrieval = None ~ Some(TrustedHelper("", "", "", "AA000000A")) ~ Some("client-id")
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.successful(authResult))

      val result =
        authAction("Some scope", Some("AB000000A")).invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe UNAUTHORIZED
    }

    "return 401(UNAUTHORIZED) when the request is not authorized" in {
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.failed(UnsupportedAuthProvider()))

      val result = authAction("Some scope").invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe UNAUTHORIZED
    }

    "return 500(INTERNAL_SERVER_ERROR) when an exception is raised, excluding AuthorisationException instances" in {
      when(
        authConnector
          .authorise(any[Predicate], any[Retrieval[AuthRetrieval]])(any[HeaderCarrier], any[ExecutionContext])
      ).thenReturn(Future.failed(new BadGatewayException("Auth service is down")))

      val result = authAction("Some scope").invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
