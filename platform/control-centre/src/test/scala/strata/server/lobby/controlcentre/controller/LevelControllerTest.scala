package strata.server.lobby.controlcentre.controller

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FlatSpec}
import org.mockito.Mockito._
import javax.servlet.http.HttpServletRequest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.yazino.platform.playerstatistic.service.PlayerStatsBackOfficeService

@RunWith(classOf[JUnitRunner])
class LevelControllerTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

  private val playerStatsBackOfficeService = mock[PlayerStatsBackOfficeService]
  private val request = mock[HttpServletRequest]


  private val underTest = new LevelController(playerStatsBackOfficeService)

  "The Controller" should "go the index" in {

    val modelAndView = underTest.index(request)

    modelAndView.getViewName should equal("game/level/index")

  }

  "The Controller" should "refresh level defintions" in {
      val modelAndView = underTest.refreshDefinitions()

      verify(playerStatsBackOfficeService).refreshLevelDefinitions();

      modelAndView.getModel().get("message") should equal("Level definitions successfully reloaded.");
  }

}
