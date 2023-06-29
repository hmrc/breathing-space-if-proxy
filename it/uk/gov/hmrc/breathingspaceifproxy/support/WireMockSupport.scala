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

package uk.gov.hmrc.breathingspaceifproxy.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.admin.model.ListStubMappingsResult
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.http.{HeaderNames, Status}
import play.mvc.Http.MimeTypes

import java.net.URL
import scala.jdk.CollectionConverters._

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  suite: Suite =>

  val wireMockPort = 12345
  val wireMockHost = "localhost"
  val wireMockBaseUrl = new URL(s"https://$wireMockHost:$wireMockPort")

  private val authUrlPath = "/auth/authorise"

  private val privilegedApplicationAuthBody = """{
                   |  "clientId": "id-123232",
                   |  "authProvider": "PrivilegedApplication",
                   |  "applicationId":"app-1",
                   |  "applicationName": "App 1",
                   |  "enrolments": ["read:breathing-space-debts","read:breathing-space-individual","read:breathing-space-periods","write:breathing-space-periods"],
                   |  "ttl": 5000
                   |}""".stripMargin

  val wireMockServer = new WireMockServer(
    wireMockConfig().port(wireMockPort)
  )

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireMockHost, wireMockPort)
    wireMockServer.start()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
    stubCall(HttpMethod.Post, authUrlPath, Status.OK, privilegedApplicationAuthBody)
  }

  def mapQueryParams(queryParams: Map[String, String]): java.util.Map[String, StringValuePattern] =
    queryParams.map { case (k, v) => k -> equalTo(v) }.asJava

  def stubCall(
    httpMethod: HttpMethod, url: String, status: Integer, body: String, queryParams: Map[String, String] = Map.empty
  ): StubMapping = {
    val call = httpMethod.call(urlPathMatching(url)).withQueryParams(mapQueryParams(queryParams))
    removeStub(call)
    stubFor {
      val response = aResponse()
        .withStatus(status)
        .withBody(body)
        .withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)

      call.willReturn(response)
    }
  }

  def unauthorized(): Unit = {
    WireMock.reset()
    stubFor {
      val response = aResponse()
        .withStatus(Status.UNAUTHORIZED)
        .withHeader("WWW-Authenticate", """MDTP detail="UnsupportedAuthProvider"""")

      post(urlPathMatching(authUrlPath)).willReturn(response)
    }
  }

  def listAllStubMappings(): ListStubMappingsResult = WireMock.listAllStubMappings()
}
