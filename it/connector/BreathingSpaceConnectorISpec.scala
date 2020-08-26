package connector

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.breathingspaceifproxy.connector.BreathingSpaceConnector
import uk.gov.hmrc.breathingspaceifproxy.model.{BsHeaders, Nino}
import uk.gov.hmrc.breathingspaceifproxy.model.Attended.PEGA_UNATTENDED
import util.WiremockHelper


class BreathingSpaceConnectorISpec(config: (String, Any)*)
  extends AnyWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with GuiceOneServerPerSuite with WiremockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config: _*)
    .build()

  private lazy val connector = app.injector.instanceOf[BreathingSpaceConnector]

  val validNino = Nino("MZ006526C")
  val validAttended = PEGA_UNATTENDED

  "BreathingSpaceConnector" should {
    "successfully make a call to the integration framework for get identity details" in {

      //TODO: need to ensure stub url and connector url matches each other
      stubFor(get(s"$url/debtor/${validNino.value}")
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("")
          )
      )

      val result = connector.requestIdentityDetails(validNino, BsHeaders("correlationId",validAttended))
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
