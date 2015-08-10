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
class AchievementControllerTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

  private val playerStatsBackOfficeService = mock[PlayerStatsBackOfficeService]
  private val request = mock[HttpServletRequest]


  private val underTest = new AchievementController(playerStatsBackOfficeService)

  "The Controller" should "go the index" in {

    val modelAndView = underTest.index(request)

    modelAndView.getViewName should equal("game/achievement/index")

  }

  "The Controller" should "refresh achievement defintions" in {
      val modelAndView = underTest.refreshDefinitions()

      verify(playerStatsBackOfficeService).refreshAchievements();

      modelAndView.getModel().get("message") should equal("Achievement definitions successfully reloaded.");
  }

}
