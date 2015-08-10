package strata.server.lobby.controlcentre.controller

import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.lang3.Validate.notNull
import org.springframework.web.servlet.ModelAndView
import org.springframework.stereotype.Controller
import org.apache.commons.logging.LogFactory
import org.springframework.web.bind.annotation._
import com.yazino.platform.table._
import scala.collection.JavaConversions._
import java.text.SimpleDateFormat
import org.joda.time.DateTime

@Controller
class CountdownController @Autowired()(val tableConfigurationUpdateService: TableConfigurationUpdateService,
                                       val tableService: TableService,
                                       val countdownService: com.yazino.platform.table.CountdownService) {
    notNull(tableConfigurationUpdateService, "tableConfigurationUpdateService may not be null")
    notNull(tableService, "tableService may not be null")
    notNull(countdownService, "countdownService may not be null")
    val LOG = LogFactory.getLog(classOf[CountdownController])
    private val DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm")
    private val ENTRY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")

    @RequestMapping(Array("maintenance/countdown/show"))
    def show(): ModelAndView = {
        var view = new ModelAndView("maintenance/countdown/show")
        val countdowns = countdownService.findAll().map {
            option => Pair(option._1, DISPLAY_DATE_FORMAT.format(option._2))
        }
        if (countdowns != null && !countdowns.isEmpty) {
            view = view.addObject("countdowns", countdowns)
        }
        view
    }

    @RequestMapping(Array("maintenance/countdown/create"))
    def create(): ModelAndView = {
        var until = new DateTime().withHourOfDay(10).withMinuteOfHour(30).withSecondOfMinute(0).withMillisOfSecond(0)
        if (until.isBeforeNow) {
            until = until.plusDays(1)
        }

        new ModelAndView("maintenance/countdown/create")
            .addObject("until", ENTRY_DATE_FORMAT.format(until.toDate))
            .addObject("gameTypes", allGameTypes())
    }

    @RequestMapping(Array("maintenance/countdown/save"))
    def save(@RequestParam("until") untilAsText: String,
             @RequestParam(value = "gameType", required = false) gameType: String): ModelAndView = {
        val until = ENTRY_DATE_FORMAT.parse(untilAsText)

        val message = if (gameType == null || "ALL".equalsIgnoreCase(gameType)) {
            tableConfigurationUpdateService.asyncPublishCountdownForAllGames(until.getTime)
            "Countdown until %s started.".format(DISPLAY_DATE_FORMAT.format(until))
        } else {
            tableConfigurationUpdateService.asyncPublishCountdownForGameType(until.getTime, gameType)
            "Countdown for %s until %s started.".format(gameType, DISPLAY_DATE_FORMAT.format(until))
        }
        show().addObject("message", message)
    }

    @RequestMapping(Array("maintenance/countdown/deactivate/{countdownId}"))
    def deactivate(@PathVariable("countdownId") countdownId: String): ModelAndView = {
        tableConfigurationUpdateService.asyncStopCountdown(countdownId)
        show().addObject("message", "Countdown %s stopped.".format(countdownId))
    }

    private def allGameTypes(): List[String] = {
        val list = new java.util.ArrayList[String]()
        for (gameType <- tableService.getGameTypes) {
            list.add(gameType.getGameType.getId)
        }
        list.toList
    }
}
