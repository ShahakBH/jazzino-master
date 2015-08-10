package strata.server.lobby.controlcentre.controller

import org.springframework.web.servlet.ModelAndView
import org.springframework.web.bind.annotation.{PathVariable, RequestParam, RequestMapping}
import org.springframework.beans.factory.annotation.Autowired
import com.yazino.platform.session.SessionService
import org.springframework.stereotype.Controller
import java.math.{BigDecimal => JavaBigDecimal}
import com.yazino.platform.community.PlayerService
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.apache.commons.lang3.Validate.notNull

@Controller
class SessionMonitorController @Autowired()(val sessionService: SessionService,
                                            val playerService: PlayerService) {
    notNull(sessionService, "sessionService may not be null")
    notNull(playerService, "playerService may not be null")

    @RequestMapping(Array("monitor/session", "monitor/session/list"))
    def list(@RequestParam(value = "page", required = false, defaultValue = "0") page: Int): ModelAndView = {
        new ModelAndView("monitor/session/list")
            .addObject("sessions", sessionService.findSessions(page))
    }

    @RequestMapping(Array("monitor/session/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int): ModelAndView = list(page - 1)

    @RequestMapping(Array("monitor/session/unload/{playerId}"))
    def unloadAll(@PathVariable("playerId") playerId: Long,
                  request: HttpServletRequest): ModelAndView = {
        sessionService.invalidateAllByPlayer(JavaBigDecimal.valueOf(playerId))
        new ModelAndView("redirect:/monitor/session/list")
    }

    @RequestMapping(Array("monitor/session/unload/{playerId}/{sessionKey}"))
    def unload(@PathVariable("playerId") playerId: Long,
               @PathVariable("sessionKey") sessionKey: String,
               request: HttpServletRequest): ModelAndView = {
        sessionService.invalidateByPlayerAndSessionKey(JavaBigDecimal.valueOf(playerId), sessionKey)
        new ModelAndView("redirect:/monitor/session/list")
    }

    @RequestMapping(Array("monitor/session/details/{playerId}/{sessionKey}"))
    def details(@PathVariable("playerId") playerId: Long,
                @PathVariable("sessionKey") sessionKey: String,
                response: HttpServletResponse): ModelAndView = {
        val player = playerService.getBasicProfileInformation(JavaBigDecimal.valueOf(playerId))
        if (player != null) {
            val session = sessionService.findByPlayerAndSessionKey(JavaBigDecimal.valueOf(playerId), sessionKey)

            new ModelAndView("monitor/session/details")
                .addObject("player", player)
                .addObject("session", session)
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

}
