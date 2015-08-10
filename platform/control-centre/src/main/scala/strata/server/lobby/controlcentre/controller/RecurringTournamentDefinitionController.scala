package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import strata.server.lobby.controlcentre.repository.{RecurringTournamentDefinitionRepository, TournamentVariationRepository}
import org.apache.commons.lang3.Validate._
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.WebDataBinder
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport
import org.springframework.beans.propertyeditors.CustomDateEditor
import java.text.SimpleDateFormat
import java.{util, math}
import strata.server.lobby.controlcentre.validation.RecurringTournamentDefinitionValidator
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import strata.server.lobby.controlcentre.form.RecurringTournamentDefinitionForm
import scala.Array
import org.springframework.validation.BindingResult
import com.yazino.platform.tournament.TournamentService

@Controller
class RecurringTournamentDefinitionController @Autowired()(val recurringTournamentDefinitionRepository: RecurringTournamentDefinitionRepository,
                                                           val tournamentVariationRepository: TournamentVariationRepository,
                                                           val tournamentService: TournamentService,
                                                           val recurringTournamentDefinitionValidator: RecurringTournamentDefinitionValidator) {
    notNull(recurringTournamentDefinitionRepository, "recurringTournamentDefinitionRepository may not be null")
    notNull(tournamentVariationRepository, "tournamentVariationRepository may not be null")
    notNull(tournamentService, "tournamentService may not be null")
    notNull(recurringTournamentDefinitionValidator, "recurringTournamentDefinitionValidator may not be null")

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())

        val editor = new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"), true)
        binder.registerCustomEditor(classOf[util.Date], editor)
    }

    @ModelAttribute("days")
    def days: util.TreeMap[Int, String] = {
        val dayMap = new util.TreeMap[Int, String]()
        dayMap.put(1, "Monday")
        dayMap.put(2, "Tuesday")
        dayMap.put(3, "Wednesday")
        dayMap.put(4, "Thursday")
        dayMap.put(5, "Friday")
        dayMap.put(6, "Saturday")
        dayMap.put(7, "Sunday")
        dayMap
    }

    @ModelAttribute("hoursOfDay")
    def hoursOfDay: util.TreeMap[Int, String] = {
        val hoursOfDayMap = new util.TreeMap[Int, String]()
        0.until(24).foreach(hour => hoursOfDayMap.put(hour, "%02d".format(hour)))
        hoursOfDayMap
    }

    @ModelAttribute("minutesOfHour")
    def minutesOfHour: util.TreeMap[Int, String] = {
        val minutesOfHourMap = new util.TreeMap[Int, String]()
        0.until(60).foreach(minute => minutesOfHourMap.put(minute, "%02d".format(minute)))
        minutesOfHourMap
    }

    @ModelAttribute("variations")
    def variations: util.TreeMap[math.BigDecimal, String] = {
        val variationsToNames = new util.TreeMap[math.BigDecimal, String]()
        tournamentVariationRepository.list().foreach {
            item => variationsToNames.put(item._1.underlying(), item._2)
        }
        variationsToNames
    }

    @RequestMapping(Array("tournament/recurring/list"))
    def list: ModelAndView = listAtPage(1)

    @RequestMapping(Array("tournament/recurring/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int = 1): ModelAndView = {
        new ModelAndView("tournament/recurring/list")
            .addObject("definitions", recurringTournamentDefinitionRepository.findAll(page - 1, 20))
    }

    @RequestMapping(Array("tournament/recurring/show/{definitionId}"))
    def show(@PathVariable("definitionId") definitionId: Int,
             response: HttpServletResponse): ModelAndView = {
        val definition = recurringTournamentDefinitionRepository.findById(BigDecimal(definitionId))

        if (definition.isDefined) {
            new ModelAndView("tournament/recurring/show")
                .addObject("definition", new RecurringTournamentDefinitionForm(definition.get))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("tournament/recurring/edit/{definitionId}"))
    def edit(@PathVariable("definitionId") definitionId: Int,
             response: HttpServletResponse): ModelAndView = {
        val modelAndView = show(definitionId, response)
        if (modelAndView != null) {
            modelAndView.setViewName("tournament/recurring/edit")
        }
        modelAndView
    }

    @RequestMapping(Array("tournament/recurring/create"))
    def create(): ModelAndView =
        new ModelAndView("tournament/recurring/create")
            .addObject("definition", new RecurringTournamentDefinitionForm())

    @RequestMapping(value = Array("tournament/recurring/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("definition") form: RecurringTournamentDefinitionForm,
             bindingResult: BindingResult): ModelAndView = {
        notNull(form, "form may not be null")

        recurringTournamentDefinitionValidator.validate(form, bindingResult)
        if (bindingResult.hasErrors) {
            val source = if (form.id != null) {
                "edit"
            } else {
                "create"
            }
            new ModelAndView("tournament/recurring/%s".format(source))
                .addObject("definition", form)

        } else {
            val savedDefinition = recurringTournamentDefinitionRepository.save(form.toDefinition)
            tournamentService.saveRecurringTournamentDefinition(
                savedDefinition.toPlatform(tournamentVariationRepository))

            new ModelAndView("redirect:/tournament/recurring/show/%s".format(savedDefinition.id))
        }
    }
}
