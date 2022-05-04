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

import play.api.Logging
import play.api.mvc.Results.NotImplemented
import play.api.mvc.{ActionBuilder, ActionFilter, AnyContent, BodyParser, ControllerComponents, Request, Result}
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

trait RequestFeatureFlag extends Logging {

  val controllerComponents: ControllerComponents
  val appConfig: AppConfig

  def enabled(pred: AppConfig => Boolean): ActionFilter[Request] with ActionBuilder[Request, AnyContent] =
    new ActionFilter[Request] with ActionBuilder[Request, AnyContent] {
      override protected def filter[A](request: Request[A]): Future[Option[Result]] =
        if (pred(appConfig))
          Future.successful(None)
        else {
          logger.debug(s"Feature disabled: ${request.path}")
          Future.successful(Some(NotImplemented))
        }

      override protected def executionContext: ExecutionContext = controllerComponents.executionContext
      override def parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser
    }
}
