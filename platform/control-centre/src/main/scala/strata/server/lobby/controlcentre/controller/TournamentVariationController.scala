package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation._
import org.apache.commons.lang3.Validate.notNull
import strata.server.lobby.controlcentre.repository.TournamentVariationRepository
import strata.server.lobby.controlcentre.form.TournamentVariationForm
import java.util
import scala.Array
import strata.server.lobby.controlcentre.validation.TournamentVariationValidator
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport
import com.yazino.platform.table.TableService
import scala.collection.JavaConversions._
import strata.server.lobby.controlcentre.model.Allocator
import TournamentVariationController._
import strata.server.lobby.controlcentre.repository.mapper.GameVariationRepository
import com.yazino.game.api.GameFeature

object TournamentVariationController {
    private val ONE_DAY = 86400000L
}

@Controller
class TournamentVariationController @Autowired()(val variationRepository: TournamentVariationRepository,
                                                 val gameVariationRepository: GameVariationRepository,
                                                 val variationValidator: TournamentVariationValidator,
                                                 val tableService: TableService) {
    notNull(variationRepository, "variationRepository may not be null")
    notNull(gameVariationRepository, "gameVariationRepository may not be null")
    notNull(variationValidator, "variationValidator may not be null")
    notNull(tableService, "tableService may not be null")

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())
    }

    @ModelAttribute("gameTypes")
    def gameTypes: util.TreeMap[String, String] = {
        val gameTypeMap = new util.TreeMap[String, String]()
        tableService.getGameTypes
            .filter { info => info.getGameType.isSupported(GameFeature.TOURNAMENT) }
            .foreach { info => gameTypeMap.put(info.getGameType.getId, info.getGameType.getName) }
        gameTypeMap
    }

    @ModelAttribute("allocators")
    def allocators: util.TreeMap[Allocator, String] = {
        val allocatorList = new util.TreeMap[Allocator, String]()
        allocatorList.put(Allocator.EVEN_BY_BALANCE, Allocator.EVEN_BY_BALANCE.getDescription)
        allocatorList.put(Allocator.EVEN_RANDOM, Allocator.EVEN_RANDOM.getDescription)
        allocatorList
    }

    @ModelAttribute("expiryDelays")
    def expiryDelays: util.TreeMap[Long, String] = {
        val expiryDelayList = new util.TreeMap[Long, String]()
        expiryDelayList.put(ONE_DAY, "One day")
        expiryDelayList.put(ONE_DAY * 2, "Two days")
        expiryDelayList.put(ONE_DAY * 3, "Three days")
        expiryDelayList.put(ONE_DAY * 4, "Four days")
        expiryDelayList.put(ONE_DAY * 5, "Five days")
        expiryDelayList.put(ONE_DAY * 6, "Six days")
        expiryDelayList.put(ONE_DAY * 7, "Seven days")
        expiryDelayList
    }

    @RequestMapping(Array("tournament/variation/list"))
    def list: ModelAndView = listAtPage(1)

    @RequestMapping(Array("tournament/variation/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int = 1): ModelAndView = {
        new ModelAndView("tournament/variation/list")
            .addObject("variations", variationRepository.findAll(page - 1, 20))
    }

    @RequestMapping(Array("tournament/variation/show/{variationId}"))
    def show(@PathVariable("variationId") variationId: Int,
             response: HttpServletResponse): ModelAndView = {
        val variation = variationRepository.findById(BigDecimal(variationId))

        if (variation.isDefined) {
            new ModelAndView("tournament/variation/show")
                .addObject("variation", new TournamentVariationForm(variation.get))
                .addObject("clients", clientsFor(variation.get.gameType))
                .addObject("gameVariations", gameVariationsFor(variation.get.gameType))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("tournament/variation/edit/{variationId}"))
    def edit(@PathVariable("variationId") variationId: Int,
             response: HttpServletResponse): ModelAndView = {
        val modelAndView = show(variationId, response)
        if (modelAndView != null) {
            modelAndView.setViewName("tournament/variation/edit")
        }
        modelAndView
    }

    @RequestMapping(Array("tournament/variation/create"))
    def create(): ModelAndView =
        new ModelAndView("tournament/variation/create")
            .addObject("variation", new TournamentVariationForm())

    @RequestMapping(value = Array("tournament/variation/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("variation") form: TournamentVariationForm,
             bindingResult: BindingResult): ModelAndView = {
        notNull(form, "form may not be null")

        variationValidator.validate(form, bindingResult)
        if (bindingResult.hasErrors) {
            if (form.id != null) {
                new ModelAndView("tournament/variation/edit")
                    .addObject("variation", form)
                    .addObject("clients", clientsFor(form.getGameType))
                    .addObject("gameVariations", gameVariationsFor(form.getGameType))
            } else {
                new ModelAndView("tournament/variation/create")
                    .addObject("variation", form)
            }

        } else {
            val savedVariation = variationRepository.save(form.toVariation)
            new ModelAndView("redirect:/tournament/variation/show/%s".format(savedVariation.id))
        }
    }

    @RequestMapping(Array("tournament/variation/delete/{variationId}"))
    def delete(@PathVariable("variationId") variationId: Int): ModelAndView = {
        variationRepository.delete(BigDecimal(variationId))

        list.addObject("message", "Tournament Variation %s has been deleted.".format(variationId))
    }

    private def gameVariationsFor(gameType: String) = {
        val variations = new util.HashMap[BigDecimal, String]()
        variations.putAll(gameVariationRepository.listFor(gameType))
        variations
    }

    private def clientsFor(gameType: String) = {
        val clientMap = new util.TreeMap[String, String]()
        tableService.findAllClientsFor(gameType).foreach {
            client => clientMap.put(client.getClientId, client.getClientId)
        }
        clientMap
    }
}
