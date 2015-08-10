package strata.server.lobby.controlcentre.util

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.math.BigDecimal

@RunWith(classOf[JUnitRunner])
class JavaBigDecimalPropertyEditorSupportTest extends FlatSpec with ShouldMatchers {

  val underTest = new JavaBigDecimalPropertyEditorSupport

  "The editor" should "return the string value of BigDecimals" in {
    underTest.setValue(new BigDecimal("12.0353454"))

    underTest.getAsText should equal("12.0353454")
  }

  it should "update the value of the internal value from a string" in {
    underTest.setAsText("4534.3434")

    underTest.getValue should equal(new BigDecimal("4534.3434"))
  }

}
