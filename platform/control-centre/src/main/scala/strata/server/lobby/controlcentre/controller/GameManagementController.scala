package strata.server.lobby.controlcentre.controller

import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.lang3.Validate.notNull
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.servlet.ModelAndView
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import org.apache.commons.logging.LogFactory
import org.springframework.web.bind.annotation._
import scala.collection.JavaConversions._
import strata.server.lobby.controlcentre.repository.GameConfigurationRepository
import com.yazino.platform.account.AccountingShutdownService
import strata.server.lobby.controlcentre.util.BigDecimalPropertyEditorSupport
import com.yazino.platform.table._

@Controller
class GameManagementController @Autowired()(val gameConfigurationRepository: GameConfigurationRepository,
                                            val tableConfigurationUpdateService: TableConfigurationUpdateService,
                                            val tableService: TableService,
                                            val accountingShutdownService: AccountingShutdownService) {
    notNull(gameConfigurationRepository, "gameConfigurationRepository may not be null")
    val LOG = LogFactory.getLog(classOf[GameConfigurationController])

    @InitBinder def initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(classOf[BigDecimal], new BigDecimalPropertyEditorSupport())
    }

    @RequestMapping(Array("game/management"))
    def index(): ModelAndView = {
        new ModelAndView("game/management/index")
            .addObject("games", allGameTypes())
    }

    @RequestMapping(Array("game/management/shutdown/all"))
    def shutdown(): ModelAndView = {
        tableConfigurationUpdateService.asyncDisableAndShutdownAllGames()
        accountingShutdownService.asyncShutdownAccounting()

        new ModelAndView("game/management/index")
            .addObject("games", allGameTypes())
            .addObject("message", "All tables have been shutdown. Account balances will be persisted immediately. All game types have been deactivated.")
    }

    @RequestMapping(Array("game/management/shutdown/{gameId}"))
    def shutdown(@PathVariable("gameId") gameId: String): ModelAndView = {
        tableConfigurationUpdateService.asyncSetAvailabilityFor(gameId, false)
        tableService.asyncShutdownGame(gameId)

        new ModelAndView("game/management/index")
            .addObject("games", allGameTypes())
            .addObject("message", "Game deactivated and tables shutdown for game %s.".format(gameId))
    }

    @RequestMapping(Array("game/management/activate/{gameId}"))
    def activate(@PathVariable("gameId") gameId: String): ModelAndView = {
        tableConfigurationUpdateService.asyncSetAvailabilityFor(gameId, true)

        new ModelAndView("game/management/index")
            .addObject("games", allGameTypes())
            .addObject("message", "Game type %s has been activated.".format(gameId))
    }

    @RequestMapping(Array("game/management/deactivate/{gameId}"))
    def deactivate(@PathVariable("gameId") gameId: String): ModelAndView = {
        tableConfigurationUpdateService.asyncSetAvailabilityFor(gameId, false)

        new ModelAndView("game/management/index")
            .addObject("games", allGameTypes())
            .addObject("message", "Game type %s has been deactivated.".format(gameId))
    }

    @RequestMapping(Array("game/management/activate/all"))
    def activateAll(): ModelAndView = {
        for (gameType <- allGameTypes()) {
            activate(gameType.getGameType.getId)
        }
        new ModelAndView("game/management/index")
            .addObject("games", allGameTypes())
            .addObject("message", "All game types have been activated.")
    }

    @RequestMapping(Array("game/management/deactivate/all"))
    def deactivateAll(): ModelAndView = {
        for (gameType <- allGameTypes()) {
            deactivate(gameType.getGameType.getId)
        }
        new ModelAndView("game/management/index")
            .addObject("games", allGameTypes())
            .addObject("message", "All game types have been deactivated.  No new games can be started.")
    }

    private def allGameTypes(): List[GameTypeInformation] = {
        val list = new java.util.ArrayList[GameTypeInformation]()
        for (gameType <- tableService.getGameTypes) {
            list.add(gameType)
        }
        list.toList.sorted(new GameTypeOrder())
    }

    class GameTypeOrder extends Ordering[GameTypeInformation] {
        override def compare(x: GameTypeInformation, y: GameTypeInformation): Int = x.getId compare y.getId
    }

}
