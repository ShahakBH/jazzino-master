package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import strata.server.lobby.controlcentre.repository.{TournamentRepository, TournamentVariationRepository}
import org.apache.commons.lang3.Validate._
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.WebDataBinder
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport
import org.springframework.beans.propertyeditors.CustomDateEditor
import java.text.SimpleDateFormat
import java.util
import strata.server.lobby.controlcentre.validation.TournamentValidator
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import strata.server.lobby.controlcentre.form.TournamentForm
import scala.Array
import org.springframework.validation.BindingResult
import com.yazino.platform.tournament.TournamentService
import java.math

@Controller
class TournamentController @Autowired()(val tournamentRepository: TournamentRepository,
                                        val tournamentVariationRepository: TournamentVariationRepository,
                                        val tournamentService: TournamentService,
                                        val tournamentValidator: TournamentValidator) {
    notNull(tournamentRepository, "tournamentRepository may not be null")
    notNull(tournamentVariationRepository, "tournamentVariationRepository may not be null")
    notNull(tournamentService, "tournamentService may not be null")
    notNull(tournamentValidator, "tournamentValidator may not be null")

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())

        val editor = new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"), true)
        binder.registerCustomEditor(classOf[util.Date], editor)
    }

    @ModelAttribute("variations")
    def variations: util.TreeMap[math.BigDecimal, String] = {
        val variationsToNames = new util.TreeMap[math.BigDecimal, String]()
        tournamentVariationRepository.list().foreach {
            item => variationsToNames.put(item._1.underlying(), item._2)
        }
        variationsToNames
    }

    @RequestMapping(Array("tournament/tournament/list"))
    def list: ModelAndView = listAtPage(1)

    @RequestMapping(Array("tournament/tournament/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int = 1): ModelAndView = {
        new ModelAndView("tournament/tournament/list")
            .addObject("tournaments", tournamentRepository.findAll(page - 1, 20))
            .addObject("cannotCancelStatuses", TournamentForm.cannotCancelStatuses)
    }

    @RequestMapping(Array("tournament/tournament/show/{tournamentId}"))
    def show(@PathVariable("tournamentId") tournamentId: Int,
             response: HttpServletResponse): ModelAndView = {
        val tournament = tournamentRepository.findById(BigDecimal(tournamentId))

        if (tournament.isDefined) {
            new ModelAndView("tournament/tournament/show")
                .addObject("tournament", new TournamentForm(tournament.get))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("tournament/tournament/create"))
    def create(): ModelAndView =
        new ModelAndView("tournament/tournament/create")
            .addObject("tournament", new TournamentForm())

    @RequestMapping(value = Array("tournament/tournament/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("tournament") form: TournamentForm,
             bindingResult: BindingResult): ModelAndView = {
        notNull(form, "form may not be null")

        tournamentValidator.validate(form, bindingResult)
        if (bindingResult.hasErrors) {
            new ModelAndView("tournament/tournament/create")
                .addObject("tournament", form)

        } else {
            val tournamentId = tournamentService.createTournament(
                form.toTournament.toPlatform(tournamentVariationRepository))

            new ModelAndView("redirect:/tournament/tournament/show/%s".format(tournamentId))
        }
    }

    @RequestMapping(Array("tournament/tournament/cancel/{tournamentId}"))
    def cancel(@PathVariable("tournamentId") tournamentId: Int): ModelAndView =
        if (tournamentService.cancelTournament(BigDecimal(tournamentId).underlying())) {
            list.addObject("message", "Tournament %s has been cancelled.".format(tournamentId))
        } else {
            list.addObject("message", "Tournament %s has not been cancelled.".format(tournamentId))
        }
}
