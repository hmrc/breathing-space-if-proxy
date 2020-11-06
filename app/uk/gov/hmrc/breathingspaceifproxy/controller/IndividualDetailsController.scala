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

import javax.inject.{Inject, Singleton}

import scala.annotation.switch
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.syntax.apply._
import cats.syntax.option._
import cats.syntax.validated._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.breathingspaceifproxy.{ResponseValidation, Validation}
import uk.gov.hmrc.breathingspaceifproxy.config.AppConfig
import uk.gov.hmrc.breathingspaceifproxy.connector.IndividualDetailsConnector
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.model.BaseError.INVALID_DETAIL_INDEX
import uk.gov.hmrc.breathingspaceifproxy.model.EndpointId._

@Singleton()
class IndividualDetailsController @Inject()(
  appConfig: AppConfig,
  cc: ControllerComponents,
  connector: IndividualDetailsConnector
) extends AbstractBaseController(appConfig, cc) {

  def get(maybeNino: String, detailId: Char): Action[Validation[AnyContent]] = Action.async(withoutBody) {
    implicit request =>
      (
        validateHeadersForNPS,
        validateNino(maybeNino),
        validateDetailId(detailId),
        request.body
      ).mapN((correlationId, nino, endpointId, _) => (RequestId(endpointId, correlationId), nino))
        .fold(HttpError(retrieveCorrelationId, BAD_REQUEST, _).send, get(detailId, _))
  }

  private def get(detailId: Char, validation: (RequestId, Nino))(implicit request: Request[_]): Future[Result] = {
    implicit val (requestId, nino) = validation
    logger.debug(s"$requestId for Nino(${nino.value}) with detailId($detailId)")
    (detailId: @switch) match {
      case '0' => evalResponse[Detail0](connector.get[Detail0](nino, DetailData0), DetailData0)
      case '1' => evalResponse[Detail1](connector.get[Detail1](nino, DetailData1), DetailData1)
      case 's' => evalResponse[IndividualDetails](connector.get[IndividualDetails](nino, FullDetails), FullDetails)

      // Shouldn't happen as detailId was already validated.
      case _ => HttpError(requestId.correlationId.toString.some, invalidDetailId(detailId)).send
    }
  }

  private def evalResponse[T <: Detail](response: ResponseValidation[T], detailData: DetailsData[T])(
    implicit requestId: RequestId
  ): Future[Result] =
    response.flatMap {
      implicit val format = detailData.format
      _.fold(HttpError(requestId.correlationId, _).send, composeResponse(OK, _))
    }

  private def validateDetailId(detailId: Int): Validation[EndpointId] =
    (detailId: @switch) match {
      case '0' => BS_Detail0_GET.validNec[ErrorItem]
      case '1' => BS_Detail1_GET.validNec[ErrorItem]
      case 's' => BS_Details_GET.validNec[ErrorItem]
      case _ => invalidDetailId(detailId).invalidNec[EndpointId]
    }

  private def invalidDetailId(detailId: Int): ErrorItem =
    ErrorItem(INVALID_DETAIL_INDEX, s" but it was '$detailId'".some)
}
