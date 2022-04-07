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

import cats.syntax.option._
import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthProviders, _}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.breathingspaceifproxy.model.{ErrorItem, HttpError}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.{INTERNAL_SERVER_ERROR, NOT_AUTHORISED}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

trait RequestAuth extends AuthorisedFunctions with Helpers with Logging {

  val controllerComponents: ControllerComponents

  val authProviders = AuthProviders(PrivilegedApplication)

  def authAction(scope: String): ActionBuilder[Request, AnyContent] =
    new ActionBuilder[Request, AnyContent] {

      override def parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser
      override protected def executionContext: ExecutionContext = controllerComponents.executionContext

      override def invokeBlock[A](request: Request[A], f: Request[A] => Future[Result]): Future[Result] = {
        val headerCarrier = HeaderCarrierConverter.fromRequest(request)
        authorised(authProviders.and(Enrolment(scope)))(f(request))(headerCarrier, executionContext)
          .recoverWith {
            case exc: AuthorisationException =>
              HttpError(retrieveCorrelationId(request), ErrorItem(NOT_AUTHORISED, exc.reason.some)).send

            case throwable: Throwable =>
              val name = throwable.getClass.getSimpleName
              logger.error(s"Exception($name) caught while authorising a request. ${throwable.getMessage}")
              HttpError(retrieveCorrelationId(request), ErrorItem(INTERNAL_SERVER_ERROR)).send
          }(executionContext)
      }
    }
}
