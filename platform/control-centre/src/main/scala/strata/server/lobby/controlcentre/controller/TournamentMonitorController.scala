package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.lang3.Validate.notNull
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.bind.annotation.{PathVariable, RequestParam, RequestMapping}
import com.yazino.platform.tournament.{TournamentService, TournamentStatus}

@Controller
class TournamentMonitorController @Autowired() (val tournamentService: TournamentService) {
    notNull(tournamentService, "tournamentService may not be null")

    @RequestMapping(Array("monitor/tournament", "monitor/tournament/list"))
    def list(@RequestParam(value = "page", required = false, defaultValue = "0") page: Int): ModelAndView = {
        new ModelAndView("monitor/tournament/list")
                .addObject("searchStatuses", TournamentStatus.values())
                .addObject("tournaments", tournamentService.findAll(page))
    }

    @RequestMapping(Array("monitor/tournament/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int): ModelAndView = list(page - 1)

    @RequestMapping(Array("monitor/tournament/list/{status}/{page}"))
    def listAtPageWithStatus(@PathVariable("status") status: String,
                             @PathVariable("page") page: Int): ModelAndView = {
        val tournamentStatus = TournamentStatus.valueOf(status.toUpperCase)
        new ModelAndView("monitor/tournament/list")
                .addObject("status", tournamentStatus)
                .addObject("searchStatuses", TournamentStatus.values())
                .addObject("tournaments", tournamentService.findByStatus(tournamentStatus, page - 1))
    }

}
