/*
 * Copyright 2025 HM Revenue & Customs
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
import cats.syntax.option.*
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.{AuthProviders, *}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.breathingspaceifproxy.connector.FandFConnector
import uk.gov.hmrc.breathingspaceifproxy.model.{AuthenticatedRequest, ErrorItem, HttpError}
import uk.gov.hmrc.breathingspaceifproxy.model.enums.BaseError.{INTERNAL_SERVER_ERROR, NOT_AUTHORISED}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

trait RequestAuth()() extends AuthorisedFunctions with Helpers with Logging {

  val controllerComponents: ControllerComponents

  val fandFConnector: FandFConnector

  private val authProviders = AuthProviders(PrivilegedApplication)

  def authAction(scope: String, requestNino: Option[String] = None)(implicit
    ec: ExecutionContext
  ): ActionBuilder[AuthenticatedRequest, AnyContent] =
    new ActionBuilder[AuthenticatedRequest, AnyContent] {

      override def parser: BodyParser[AnyContent]               = controllerComponents.parsers.defaultBodyParser
      override protected def executionContext: ExecutionContext = controllerComponents.executionContext

      override def invokeBlock[A](request: Request[A], f: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
        val notAuthorised = HttpError(retrieveCorrelationId(request), ErrorItem(NOT_AUTHORISED, None)).send
        val headerCarrier = HeaderCarrierConverter.fromRequest(request)

        authorised(ConfidenceLevel.L200.or(authProviders.and(Enrolment(scope))))
          .retrieve(nino.and(clientId)) {
            case Some(authNino) ~ None =>
              fandFConnector.getTrustedHelper()(headerCarrier).flatMap { maybeTrustedHelper =>
                val principalNino = maybeTrustedHelper.flatMap(_.principalNino)

                (requestNino, principalNino) match {
                  case (Some(reqNino), Some(prinNino)) if reqNino == prinNino =>
                    f(AuthenticatedRequest(request, Some(prinNino), None))
                  case (Some(reqNino), None) if reqNino == authNino           =>
                    f(AuthenticatedRequest(request, Some(authNino), None))
                  case (Some(_), None)                                        =>
                    logger.error("The nino in the request does not match the authenticated nino")
                    notAuthorised
                  case (Some(_), Some(_))                                     =>
                    logger.error("The nino in the request does not match the principal nino")
                    notAuthorised
                  case _                                                      =>
                    logger.error("No nino in the request")
                    notAuthorised
                }
              }
            case None ~ Some(clientId) => f(AuthenticatedRequest(request, None, Some(clientId)))
            case _                     => notAuthorised
          }(headerCarrier, executionContext)
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
