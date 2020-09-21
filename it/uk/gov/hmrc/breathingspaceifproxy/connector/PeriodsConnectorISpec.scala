package uk.gov.hmrc.breathingspaceifproxy.connector

import java.time.{LocalDate, ZonedDateTime}

import play.api.http.Status
import play.api.test.Helpers.await
import uk.gov.hmrc.breathingspaceifproxy.model._
import uk.gov.hmrc.breathingspaceifproxy.support.{BaseISpec, HttpMethod, TestData}

class PeriodsConnectorISpec extends BaseISpec {

  val ExampleNino = Nino("MG34567")
  lazy val connector = app.injector.instanceOf[PeriodsConnector]


  s"PeriodsConnector.parseIFPostBreathingSpaceResponse" should {
    "return a Right[IFCreatePeriodsResponse] when it receives a 201 response" ignore {
      stubCall(HttpMethod.Post, PeriodsConnector.url(ExampleNino), Status.CREATED, validCreatePeriodsResponse)

      implicit val headerSet = RequiredHeaderSet(
        CorrelationId(""),
        Attended.DS2_BS_UNATTENDED,
        StaffId.UnattendedRobotValue
      )

      await(connector.post(ValidatedCreatePeriodsRequest(ExampleNino, List(Period(LocalDate.now(), Some(LocalDate.now()), ZonedDateTime.now())))))
    }

    "return a Left[ErrorResponse] when it receives any other status than 201" ignore {}
  }


}
