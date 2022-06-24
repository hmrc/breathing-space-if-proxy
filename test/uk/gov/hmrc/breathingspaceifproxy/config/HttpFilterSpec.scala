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

package uk.gov.hmrc.breathingspaceifproxy.config

import akka.stream.Materializer
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.{DefaultActionBuilder, EssentialAction}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{call, headers}
import uk.gov.hmrc.breathingspaceifproxy.support.BaseSpec

class HttpFilterSpec extends AnyWordSpec with BaseSpec {
  val httpFilter = inject[HttpFilter]
  override implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val Action = app.injector.instanceOf(classOf[DefaultActionBuilder])

  "HttpFilter" should {
    "return a request with a cache-control header" in {
      val action: EssentialAction = Action(_ => Ok(""))
      val request = FakeRequest().withBody("")
      val result = call(action, request)
      val response = httpFilter.apply(_ => result)(request)

      headers(response).get("Cache-Control") shouldBe Some(appConfig.httpHeaderCacheControl)
    }

    "replace the Cache-Control header" in {
      val action: EssentialAction = Action(_ => Ok("").withHeaders(Seq(("Cache-Control", "something")): _*))
      val request = FakeRequest().withBody("")
      val result = call(action, request)
      val response = httpFilter.apply(_ => result)(request)

      headers(response).get("Cache-Control") shouldBe Some(appConfig.httpHeaderCacheControl)
    }
  }
}
