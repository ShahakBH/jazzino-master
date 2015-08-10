package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.web.servlet.ModelAndView
import strata.server.lobby.controlcentre.repository.GameVariationPropertyOptionRepository
import org.springframework.web.bind.annotation._
import strata.server.lobby.controlcentre.form.{GameVariationPropertyForm, GameVariationForm}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import collection.JavaConversions._
import strata.server.lobby.controlcentre.model.{GameVariation, GameVariationPropertyOption}
import org.springframework.web.bind.WebDataBinder
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport
import com.yazino.platform.table.{TableService, TableConfigurationUpdateService}
import org.springframework.beans.factory.annotation.Autowired
import strata.server.lobby.controlcentre.repository.mapper.GameVariationRepository
import org.apache.commons.lang3.Validate.notNull
import GameVariationController.defaultPageSize

object GameVariationController {
    private val defaultPageSize = 20
}

@Controller
class GameVariationController @Autowired()(val gameVariationRepository: GameVariationRepository,
                                           val gameVariationPropertyOptionRepository: GameVariationPropertyOptionRepository,
                                           val tableService: TableService,
                                           val tableConfigurationUpdateService: TableConfigurationUpdateService) {
    notNull(gameVariationRepository, "gameVariationRepository may not be null")
    notNull(gameVariationPropertyOptionRepository, "gameVariationPropertyOptionRepository may not be null")
    notNull(tableService, "tableService may not be null")
    notNull(tableConfigurationUpdateService, "tableConfigurationUpdateService may not be null")

    @InitBinder def  initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())
    }

    @RequestMapping(Array("game/variation", "game/variation/list"))
    def list(@RequestParam(value = "page", required = false, defaultValue = "0") page: Int,
             @RequestParam(value = "pageSize", required = false, defaultValue = "20") pageSize: Int = defaultPageSize): ModelAndView = {
        new ModelAndView("game/variation/list")
                .addObject("gameVariations", gameVariationRepository.findAll(page, pageSize))
    }

    @RequestMapping(Array("game/variation/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int): ModelAndView = list(page - 1)

    @RequestMapping(Array("game/variation/show/{gameId}"))
    def show(@PathVariable("gameId") userId: Int,
             response: HttpServletResponse): ModelAndView = {
        val gameVariation = gameVariationRepository.findById(userId)

        if (gameVariation.isDefined) {
            new ModelAndView("game/variation/show")
                    .addObject("gameVariation", new GameVariationForm(gameVariation.get))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("game/variation/create"))
    def create: ModelAndView = {
        val gameTypes = List[String]() ++ tableService.getGameTypes.map {
            _.getId
        }
        new ModelAndView("game/variation/create")
                .addObject("gameTypes", gameTypes.sortWith((e1, e2) => (e1 compareTo e2) < 0))
                .addObject()
    }

    @RequestMapping(Array("game/variation/create/{gameType}"))
    def createWithGameType(@PathVariable("gameType") gameType: String): ModelAndView = {
        val propertyOptions = gameVariationPropertyOptionRepository.optionsFor(gameType)
        new ModelAndView("game/variation/create")
                .addObject("propertyOptions", mapByName(propertyOptions))
                .addObject("gameVariation", createVariation(gameType, propertyOptions))
    }

    @RequestMapping(Array("game/variation/edit/{gameVariationId}"))
    def edit(@PathVariable("gameVariationId") gameVariationId: Long,
             response: HttpServletResponse): ModelAndView = {
        val gameVariation = gameVariationRepository.findById(gameVariationId)
        if (gameVariation.isDefined) {
            val propertyOptions = gameVariationPropertyOptionRepository.optionsFor(gameVariation.get.gameType)
            new ModelAndView("game/variation/edit")
                    .addObject("propertyOptions", mapByName(propertyOptions))
                    .addObject("gameVariation", updateVariationWithMissingProperties(gameVariation.get, propertyOptions))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(value = Array("game/variation/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("gameVariation") form: GameVariationForm,
             request: HttpServletRequest): ModelAndView = {
        notNull(form, "form may not be null")

        val savedVariation = gameVariationRepository.save(form.toGameVariation)
        tableConfigurationUpdateService.asyncRefreshTemplates()
        new ModelAndView("redirect:/game/variation/show/%s".format(savedVariation.id))
    }

    @RequestMapping(Array("game/variation/delete/{gameVariationId}"))
    def delete(@PathVariable("gameVariationId") gameVariationId: Long,
               request: HttpServletRequest): ModelAndView = {
        gameVariationRepository.delete(gameVariationId)
        new ModelAndView("redirect:/game/variation/list")
    }

    private def mapByName(propertyOptions: List[GameVariationPropertyOption]) =
        propertyOptions.foldLeft(Map[String, GameVariationPropertyOption]()) {
            (map, option) => map + (option.propertyName -> option)
        }

    private def updateVariationWithMissingProperties(gameVariation: GameVariation, propertyOptions: List[GameVariationPropertyOption]) = {
        val propertyNamesInVariation = gameVariation.properties.map { _.name }
        val mergedProperties = gameVariation.properties.map { new GameVariationPropertyForm(_) } ++
                propertyOptions.foldLeft(List[GameVariationPropertyForm]()) { (list, option) =>
            if (propertyNamesInVariation.contains(option.propertyName)) {
                list
            } else {
                new GameVariationPropertyForm(null, option.propertyName, option.defaultValue) :: list
            }
        }

        new GameVariationForm(gameVariation.id, gameVariation.gameType, gameVariation.name,
                mergedProperties.sortWith((e1, e2) => (e1.name compareTo e2.name) < 0))
    }

    private def createVariation(gameType: String, propertyOptions: List[GameVariationPropertyOption]) = {
        val properties = propertyOptions.map {
            option =>
                new GameVariationPropertyForm(null, option.propertyName, option.defaultValue)
        }

        new GameVariationForm(null, gameType, "", properties.sortWith((e1, e2) => (e1.name compareTo e2.name) < 0))
    }

}
