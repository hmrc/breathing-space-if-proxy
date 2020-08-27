package connector

import controller.BaseControllerISpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.breathingspaceifproxy.connector.BreathingSpaceConnector
import uk.gov.hmrc.breathingspaceifproxy.model.{BsHeaders, Nino}
import uk.gov.hmrc.breathingspaceifproxy.model.Attended.PEGA_UNATTENDED
import util.WiremockHelper


class BreathingSpaceConnectorISpec(config: (String, Any)*)
  extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach with WiremockHelper {

  private lazy val connector = app.injector.instanceOf[BreathingSpaceConnector]

  val validNino = Nino("MZ006526C")
  val validAttended = PEGA_UNATTENDED

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

  "BreathingSpaceConnector" should {
    "successfully make a call to the integration framework for get identity details" in {

      //TODO: need to ensure stub url and connector url matches each other
      /*stubFor(get(s"$url/debtor/${validNino.value}")
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("")
          )
      )*/

      stubGet(s"$url/debtor/${validNino.value}", 200, "")

      val result = await(connector.requestIdentityDetails(validNino, BsHeaders("correlationId",validAttended)))

      result.status shouldBe Status.OK
    }
  }
}
