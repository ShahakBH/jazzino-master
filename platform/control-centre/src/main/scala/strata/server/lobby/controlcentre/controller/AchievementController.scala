package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.yazino.platform.playerstatistic.service.PlayerStatsBackOfficeService
import org.springframework.web.bind.annotation.{RequestMethod, RequestMapping}
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest

@Controller
class AchievementController @Autowired()(val playerStatsBackOfficeService: PlayerStatsBackOfficeService) {

  @RequestMapping(Array("game/achievement", "game/achievement/index"))
  def index(request: HttpServletRequest): ModelAndView = {
    new ModelAndView("game/achievement/index")
  }

  @RequestMapping(value = Array("/game/achievement/refresh"), method = Array(RequestMethod.POST))
  def refreshDefinitions(): ModelAndView = {
    playerStatsBackOfficeService.refreshAchievements()
    new ModelAndView("game/achievement/index")
      .addObject("message", "Achievement definitions successfully reloaded.")
  }
}
