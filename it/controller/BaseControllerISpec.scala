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

package controller

import java.io.File

import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.inject.bind

abstract class BaseControllerISpec(config: (String, Any)*) extends AnyWordSpec with Matchers with GuiceOneServerPerSuite {

  val confFile = new File("conf/application.conf")
  val parsedConfig = ConfigFactory.parseFile(confFile)
  implicit val configFromFile = Configuration(ConfigFactory.load(parsedConfig))

  private val serviceConfig = new ServicesConfig(configFromFile)

  override implicit lazy val app: Application = GuiceApplicationBuilder(configuration = configFromFile)
    .overrides(bind[ServicesConfig].to(serviceConfig))
    .build()
  /*new GuiceApplicationBuilder()
    .configure(configFromFile)
//    .configure(("microservice.integration-framework.host" -> "localhost"))
    .build()*/

  override def fakeApplication(): Application = app

  implicit val configuration: Configuration = app.configuration

  protected implicit val materializer: Materializer = app.materializer

  implicit val ws = StandaloneAhcWSClient()
}
