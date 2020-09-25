package uk.gov.hmrc.breathingspaceifproxy.connector

import java.time.{LocalDate, ZonedDateTime}

import cats.syntax.option._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod}

class PeriodsConnectorISpec extends BaseISpec {

  lazy val connector = app.injector.instanceOf[PeriodsConnector]

  val vcpr = ValidatedCreatePeriodsRequest(
    invalidNino,
    List(RequestPeriod(
      LocalDate.now(),
      LocalDate.now().some,
      ZonedDateTime.now()
    ))
  )

  s"PeriodsConnector.parseIFPostBreathingSpaceResponse" should {
    "return a Right[IFCreatePeriodsResponse] when it receives a 201 response" ignore {
      stubCall(
        HttpMethod.Post,
        PeriodsConnector.url(invalidNino),
        Status.CREATED,
        Json.obj("periods" -> vcpr.periods).toString
      )

      await(connector.post(vcpr))
    }

    "return a Left[ErrorResponse] when it receives any other status than 201" ignore {}
  }
}
