package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.yazino.platform.playerstatistic.service.PlayerStatsBackOfficeService
import org.springframework.web.bind.annotation.{RequestMethod, RequestMapping}
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest

@Controller
class LevelController @Autowired()(val playerStatsBackOfficeService: PlayerStatsBackOfficeService) {

    @RequestMapping(Array("game/level", "game/level/index"))
    def index(request: HttpServletRequest): ModelAndView = {
        new ModelAndView("game/level/index")
    }

    @RequestMapping(value = Array("/game/level/refresh"), method = Array(RequestMethod.POST))
    def refreshDefinitions(): ModelAndView = {
        playerStatsBackOfficeService.refreshLevelDefinitions()
        new ModelAndView("game/level/index")
            .addObject("message", "Level definitions successfully reloaded.")
    }
}
