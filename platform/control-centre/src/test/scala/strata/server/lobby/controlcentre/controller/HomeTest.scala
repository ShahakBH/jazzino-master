package strata.server.lobby.controlcentre.controller

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HomeTest extends FlatSpec with ShouldMatchers {

    val underTest = new Home

    "The controller" should "return the view 'home'" in {
        val modelAndView = underTest.home()

        modelAndView.getViewName should equal ("home")
    }

    it should "return 'hello' as 'world'" in {
        val modelAndView = underTest.home()

        modelAndView.getModel.get("hello") should equal ("world")
    }

}
