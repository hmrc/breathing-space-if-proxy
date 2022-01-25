/*
 * Copyright 2022 HM Revenue & Customs
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

import scala.concurrent.{ExecutionContext, Future}

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
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier}

class RequestAuthSpec extends AnyWordSpec with BaseSpec with RequestAuth with Results {

  val controllerComponents = PlayHelpers.stubControllerComponents()

  "authAction" should {

    "return 200(OK) when the request is authorized" in {
      val result = authAction("Some scope").invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe OK
    }

    "return 401(UNAUTHORIZED) when the request is not authorized" in {
      when(authConnector.authorise(any[Predicate], any[Retrieval[Unit]])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(UnsupportedAuthProvider()))

      val result = authAction("Some scope").invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe UNAUTHORIZED
    }

    "return 500(INTERNAL_SERVER_ERROR) when an exception is raised, excluding AuthorisationException instances" in {
      when(authConnector.authorise(any[Predicate], any[Retrieval[Unit]])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(new BadGatewayException("Auth service is down")))

      val result = authAction("Some scope").invokeBlock[AnyContent](fakeGetRequest, _ => Future.successful(Ok))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
