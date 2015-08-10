package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import com.yazino.platform.community.{Trophy, CommunityConfigurationUpdateService, TrophyService}
import org.apache.commons.lang3.Validate._
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import scala.Array
import org.springframework.validation.BindingResult
import org.slf4j.LoggerFactory
import strata.server.lobby.controlcentre.form.TrophyForm
import com.yazino.platform.table.TableService
import strata.server.lobby.controlcentre.validation.TrophyValidator
import com.yazino.platform.model.PagedData
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Value
import java.util
import org.springframework.web.bind.WebDataBinder
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport
import TrophyController.defaultPageSize

object TrophyController {
    private val defaultPageSize = 20
}

@Controller
class TrophyController @Autowired()(val trophyService: TrophyService,
                                    val tableService: TableService,
                                    val communityConfigurationUpdateService: CommunityConfigurationUpdateService,
                                    val trophyValidator: TrophyValidator,
                                    @Value("${senet.web.content}") val senetWebContent: String) {
    notNull(trophyService, "trophyService may not be null")
    notNull(tableService, "tableService may not be null")
    notNull(communityConfigurationUpdateService, "communityConfigurationUpdateService may not be null")
    notNull(trophyValidator, "trophyValidator may not be null")

    private val logger = LoggerFactory.getLogger(classOf[TrophyController])

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())
    }

    @ModelAttribute("gameTypes")
    def gameTypes: util.HashMap[String, String] = {
        val gameTypeMap = new util.HashMap[String, String]()
        tableService.getGameTypes.foreach(info => gameTypeMap.put(info.getGameType.getId, info.getGameType.getName))
        gameTypeMap
    }

    @ModelAttribute("assetUrl")
    def assetUrl: String = senetWebContent

    @RequestMapping(Array("tournament/trophy/list"))
    def list: ModelAndView = listAtPage(1)

    @RequestMapping(Array("tournament/trophy/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int = 1): ModelAndView = {
        new ModelAndView("tournament/trophy/list")
            .addObject("trophies", paginate(trophyService.findAll(), page - 1, defaultPageSize))
    }

    @RequestMapping(Array("tournament/trophy/show/{trophyId}"))
    def show(@PathVariable("trophyId") trophyId: Int,
             response: HttpServletResponse): ModelAndView = {
        val trophy = trophyService.findById(BigDecimal(trophyId).underlying())

        if (trophy != null) {
            new ModelAndView("tournament/trophy/show")
                .addObject("trophy", new TrophyForm(trophy))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("tournament/trophy/edit/{trophyId}"))
    def edit(@PathVariable("trophyId") trophyId: Int,
             response: HttpServletResponse): ModelAndView = {
        val modelAndView = show(trophyId, response)
        if (modelAndView != null) {
            modelAndView.setViewName("tournament/trophy/edit")
        }
        modelAndView
    }

    @RequestMapping(Array("tournament/trophy/create"))
    def create(): ModelAndView =
        new ModelAndView("tournament/trophy/create")
            .addObject("trophy", new TrophyForm())

    @RequestMapping(value = Array("tournament/trophy/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("trophy") form: TrophyForm,
             bindingResult: BindingResult): ModelAndView = {
        notNull(form, "form may not be null")

        trophyValidator.validate(form, bindingResult)
        if (bindingResult.hasErrors) {
            val source = if (form.id != null) {
                "edit"
            } else {
                "create"
            }
            new ModelAndView("tournament/trophy/%s".format(source))
                .addObject("trophy", form)

        } else {
            if (form.id == null) {
                form.id = trophyService.create(form.toTrophy)
            } else {
                trophyService.update(form.toTrophy)
            }

            refreshTrophiesInGrid()

            new ModelAndView("redirect:/tournament/trophy/show/%s".format(form.id))
        }
    }

    private def refreshTrophiesInGrid() {
        logger.debug("Refreshing trophies in grid")

        communityConfigurationUpdateService.refreshTrophies()
    }

    private def paginate(items: Iterable[Trophy], page: Int = 0, pageSize: Int = defaultPageSize) = {
        val pageOfItems = items.slice(page * pageSize, (page * pageSize) + pageSize).toList
        new PagedData[Trophy](page * pageSize, pageOfItems.size, items.size, pageOfItems)
    }
}
