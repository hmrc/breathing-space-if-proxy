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

package uk.gov.hmrc.breathingspaceifproxy.connector.test

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.JsValue
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class IndividualConnector @Inject()(http: HttpClient)(
  implicit appConfig: AppConfig,
  ec: ExecutionContext
) extends TestConnectorHelper {

  def count(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.GET[HttpResponse](Url(url("/count")).value)

  def delete(nino: Nino)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.DELETE[HttpResponse](Url(url(s"/${nino.value}")).value)

  def deleteAll(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.DELETE[HttpResponse](Url(url("/wipe-all")).value)

  def exists(nino: Nino)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.GET[HttpResponse](Url(url(s"/${nino.value}")).value)

  def listOfNinos(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.GET[HttpResponse](Url(url("/ninos")).value)

  def postIndividual(payload: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.POST[JsValue, HttpResponse](Url(url("/single")).value, payload)

  def postIndividuals(payload: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.POST[JsValue, HttpResponse](Url(url("/bulk")).value, payload)

  def putIndividual(nino: Nino, payload: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.PUT[JsValue, HttpResponse](Url(url(s"/${nino.value}")).value, payload)
}
