package uk.gov.hmrc.breathingspaceifproxy.connector

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.breathingspaceifproxy.model.{BsHeaders, Nino}
import uk.gov.hmrc.breathingspaceifproxy.model.Attended.PEGA_UNATTENDED
import uk.gov.hmrc.breathingspaceifproxy.utils.BaseControllerISpec
import util.WiremockHelper

class BreathingSpaceConnectorISpec
  extends BaseControllerISpec
    with BeforeAndAfterAll with BeforeAndAfterEach with WiremockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.integration-framework.port" -> 11111) //TODO: why does making this value use 'wiremockPort' cause the connector to use port 0 ????
    .build()

  val context = configuration.get[String]("microservice.services.integration-framework.context")

  private lazy val connector = app.injector.instanceOf[BreathingSpaceConnector]

  val validNino = Nino("MZ006526C")
  val validAttended = PEGA_UNATTENDED

  "BreathingSpaceConnector" should {
    "successfully make a call to the integration framework for get identity details" in {

      stubGet(s"/$context/debtor/${validNino.value}", Status.OK, "")

      val result = await(connector.requestIdentityDetails(validNino, BsHeaders("correlationId", validAttended)))
      result.status shouldBe Status.OK
    }

    "handle a non 2XX response from the integration framework" in {
      stubGet(s"/$context/debtor/${validNino.value}", Status.NOT_FOUND, "")

      val result = await(connector.requestIdentityDetails(validNino, BsHeaders("correlationId", validAttended)))
      result.status shouldBe Status.NOT_FOUND
    }
  }

  override def beforeEach(): Unit = {
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }
}
