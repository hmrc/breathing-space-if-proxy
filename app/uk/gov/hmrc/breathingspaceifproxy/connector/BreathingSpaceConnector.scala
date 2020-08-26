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

package uk.gov.hmrc.breathingspaceifproxy.connector
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.model.{BsHeaders, Nino}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._

class BreathingSpaceConnector @Inject()(http: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends HttpErrorFunctions
    with Logging {

  def requestIdentityDetails(nino: Nino, bsHeaders: BsHeaders): Future[HttpResponse] = {
    val url = s"${appConfig.integrationFrameworkUrl}/debtor/${nino.value}"

    implicit val hc: HeaderCarrier = constructHeaders(bsHeaders)

    http
      .GET[HttpResponse](url)
      .map { response =>
        response.status match {
          case status if is2xx(status) =>
            logger.debug(s"Received back expected status of $status calling $url")
            response

          case status => // 1xx, 3xx, 4xx, 5xx
            logger.error(s"Received back unexpected status of $status calling $url ")
            logger.debug(s"Received back body '${response.body}'")
            response
        }
      }
      .recoverWith {
        case httpError: HttpException =>
          logger.error(
            s"Call to Integration Framework failed. url=$url HttpStatus=${httpError.responseCode} error=${httpError.getMessage}"
          )
          Future.failed(httpError)

        case e: Throwable =>
          logger.error(s"Call to Integration Framework failed. url=$url", e)
          Future.failed(e)
      }
  }

  private def constructHeaders(bsHeaders: BsHeaders): HeaderCarrier = {
    val hc = HeaderCarrier().withExtraHeaders(
      "X-Correlation-Id" -> bsHeaders.correlationId,
      "X-Context" -> bsHeaders.context.toString
    )

    bsHeaders.clientId.fold(hc)(clientId => hc.withExtraHeaders("X-Client-Id" -> clientId))
  }
}
