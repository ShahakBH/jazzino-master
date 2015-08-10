package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation._
import org.apache.commons.lang3.Validate.notNull
import strata.server.lobby.controlcentre.form.TrophyLeaderboardForm
import scala.Array
import strata.server.lobby.controlcentre.validation.TrophyLeaderboardValidator
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport
import com.yazino.platform.community.TrophyService
import java.text.SimpleDateFormat
import org.springframework.beans.propertyeditors.CustomDateEditor
import java.{math, util}
import com.yazino.platform.tournament.{TrophyLeaderboardView, TrophyLeaderboardService}
import com.yazino.platform.table.TableService
import com.yazino.platform.model.PagedData
import scala.collection.JavaConversions._
import TrophyLeaderboardController.defaultPageSize
import com.yazino.game.api.GameFeature

object TrophyLeaderboardController {
    private val defaultPageSize = 20
}

@Controller
class TrophyLeaderboardController @Autowired()(val trophyLeaderboardService: TrophyLeaderboardService,
                                               val tableService: TableService,
                                               val trophyService: TrophyService,
                                               val trophyLeaderboardValidator: TrophyLeaderboardValidator) {
    notNull(trophyLeaderboardService, "trophyLeaderboardService may not be null")
    notNull(tableService, "tableService may not be null")
    notNull(trophyService, "trophyService may not be null")
    notNull(trophyLeaderboardValidator, "trophyLeaderboardValidator may not be null")

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())

        val editor = new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true)
        binder.registerCustomEditor(classOf[util.Date], editor)
    }

    @ModelAttribute("gameTypes")
    def gameTypes: util.TreeMap[String, String] = {
        val gameTypeMap = new util.TreeMap[String, String]()
        tableService.getGameTypes
            .filter { info => info.getGameType.isSupported(GameFeature.TOURNAMENT) }
            .foreach { info => gameTypeMap.put(info.getGameType.getId, info.getGameType.getName) }
        gameTypeMap
    }

    @RequestMapping(Array("tournament/leaderboard/list"))
    def list: ModelAndView = listAtPage(1)

    @RequestMapping(Array("tournament/leaderboard/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int = 1): ModelAndView = {
        new ModelAndView("tournament/leaderboard/list")
            .addObject("leaderboards", paginate(trophyLeaderboardService.findAll(), page - 1, defaultPageSize))
    }

    @RequestMapping(Array("tournament/leaderboard/show/{leaderboardId}"))
    def show(@PathVariable("leaderboardId") leaderboardId: Int,
             response: HttpServletResponse): ModelAndView = {
        val leaderboard = trophyLeaderboardService.findById(BigDecimal(leaderboardId).underlying())

        if (leaderboard != null) {
            new ModelAndView("tournament/leaderboard/show")
                .addObject("leaderboard", new TrophyLeaderboardForm(leaderboard))
                .addObject("trophies", trophiesFor(leaderboard.getGameType))
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            null
        }
    }

    @RequestMapping(Array("tournament/leaderboard/edit/{leaderboardId}"))
    def edit(@PathVariable("leaderboardId") leaderboardId: Int,
             response: HttpServletResponse): ModelAndView = {
        val modelAndView = show(leaderboardId, response)
        if (modelAndView != null) {
            modelAndView.setViewName("tournament/leaderboard/edit")
        }
        modelAndView
    }

    @RequestMapping(Array("tournament/leaderboard/create"))
    def create(): ModelAndView =
        new ModelAndView("tournament/leaderboard/create")
            .addObject("leaderboard", new TrophyLeaderboardForm())

    @RequestMapping(value = Array("tournament/leaderboard/save"), method = Array(RequestMethod.POST))
    def save(@ModelAttribute("leaderboard") form: TrophyLeaderboardForm,
             bindingResult: BindingResult): ModelAndView = {
        notNull(form, "form may not be null")

        trophyLeaderboardValidator.validate(form, bindingResult)
        if (bindingResult.hasErrors) {
            if (form.id != null) {
                new ModelAndView("tournament/leaderboard/edit")
                    .addObject("leaderboard", form)
                    .addObject("trophies", trophiesFor(form.gameType))
            } else {
                new ModelAndView("tournament/leaderboard/create")
                    .addObject("leaderboard", form)
            }

        } else {
            val leaderboardId = if (form.id != null) {
                trophyLeaderboardService.update(populateViewWithExistingData(form.toView))
                form.id
            } else {
                trophyLeaderboardService.create(form.toDefinition)
            }

            new ModelAndView("redirect:/tournament/leaderboard/show/%s".format(leaderboardId))
        }
    }

    @RequestMapping(Array("tournament/leaderboard/disable/{leaderboardId}"))
    def disable(@PathVariable("leaderboardId") leaderboardId: Int): ModelAndView = {
        trophyLeaderboardService.setIsActive(BigDecimal(leaderboardId).underlying(), false)

        list.addObject("message", "Leaderboard %s has been disabled.".format(leaderboardId))
    }

    @RequestMapping(Array("tournament/leaderboard/enable/{leaderboardId}"))
    def enable(@PathVariable("leaderboardId") leaderboardId: Int): ModelAndView = {
        trophyLeaderboardService.setIsActive(BigDecimal(leaderboardId).underlying(), true)

        list.addObject("message", "Leaderboard %s has been enabled.".format(leaderboardId))
    }

    private def trophiesFor(gameType: String) = {
        val trophyMap = new util.HashMap[math.BigDecimal, String]()
        trophyService.findForGameType(gameType).foreach(trophy => trophyMap.put(trophy.getId, trophy.getName))
        trophyMap.put(new math.BigDecimal(-1), "None")
        trophyMap
    }

    private def populateViewWithExistingData(view: TrophyLeaderboardView) = {
        val existingView = trophyLeaderboardService.findById(view.getId)
        if (existingView == null) {
            throw new IllegalArgumentException("Leaderboard %s does not exist".format(view.getId))
        }
        new TrophyLeaderboardView(view.getId, view.getName, view.getActive,
            view.getGameType, view.getPointBonusPerPlayer, view.getStartTime, view.getEndTime,
            existingView.getCurrentCycleEnd, view.getCycle, view.getPositionData, existingView.getPlayers)
    }

    private def paginate(items: Iterable[TrophyLeaderboardView], page: Int = 0, pageSize: Int = defaultPageSize) = {
        val pageOfItems = items.slice(page * pageSize, (page * pageSize) + pageSize).toList
        new PagedData[TrophyLeaderboardView](page * pageSize, pageOfItems.size, items.size, pageOfItems)
    }

}
