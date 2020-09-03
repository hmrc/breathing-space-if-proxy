/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.breathingspaceifproxy.controller
import javax.inject.Inject

import scala.concurrent.ExecutionContext

import controllers.Assets
import play.api.{Configuration, Logging}
import play.api.http.MimeTypes
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

class ApiPlatformController @Inject()(assets: Assets, cc: ControllerComponents, configuration: Configuration)(
  implicit val ec: ExecutionContext
) extends BackendController(cc)
    with Logging {

  private lazy val v1WhitelistedApplicationIds =
    configuration.get[Seq[String]]("api.access.version-1.0.whitelistedApplicationIds")

  def getDefinition(): Action[AnyContent] = Action {
    logger.debug(s"ApiPlatformController definition endpoint has been called")
    Ok(uk.gov.hmrc.breathingspaceifproxy.views.txt.definition(v1WhitelistedApplicationIds)).as(MimeTypes.JSON)
  }

  def conf(version: String, file: String): Action[AnyContent] =
    assets.at(s"/public/api/conf/$version", file)
}
