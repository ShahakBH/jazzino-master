package strata.server.lobby.controlcentre.controller

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.lang3.Validate.notNull
import org.springframework.web.servlet.ModelAndView
import com.yazino.platform.table.{TableSearchOption, TableType, TableService}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import java.math.{BigDecimal => JavaBigDecimal}
import javax.servlet.http.HttpServletResponse

@Controller
class TableMonitorController @Autowired()(val tableService: TableService) {
    notNull(tableService, "tableService may not be null")

    @ModelAttribute("tableTypes")
    def tableTypes: Array[TableType] = TableType.values()

    @RequestMapping(Array("monitor/table", "monitor/table/list"))
    def list(@RequestParam(value = "page", required = false, defaultValue = "0") page: Int): ModelAndView = {
        new ModelAndView("monitor/table/list")
            .addObject("tables", tableService.findByType(TableType.ALL, page))
    }

    @RequestMapping(Array("monitor/table/list/{page}"))
    def listAtPage(@PathVariable("page") page: Int): ModelAndView = list(page - 1)

    @RequestMapping(Array("monitor/table/list/{type}/{page}"))
    def listAtPageWithOptions(@PathVariable("type") desiredType: String,
                              @PathVariable("page") page: Int,
                              @RequestParam(value = "onlyWithPlayers", required = false, defaultValue = "false") onlyWithPlayers: Boolean,
                              @RequestParam(value = "showState", required = false) showState: String): ModelAndView = {
        val tableType = TableType.valueOf(desiredType.toUpperCase)

        var searchOptions: List[TableSearchOption] = List()

        if (showState != null) {
            if (showState == "error") {
                searchOptions ::= TableSearchOption.IN_ERROR_STATE
            } else if (showState == "open") {
                searchOptions ::= TableSearchOption.ONLY_OPEN
            }
        }

        if (onlyWithPlayers) {
            searchOptions ::= TableSearchOption.ONLY_WITH_PLAYERS
        }

        new ModelAndView("monitor/table/list")
            .addObject("tableType", tableType)
            .addObject("showState", showState)
            .addObject("onlyWithPlayers", onlyWithPlayers)
            .addObject("tables", tableService.findByType(tableType, page - 1, searchOptions: _*))
    }

    @RequestMapping(Array("monitor/table/loadAll"))
    def loadAll(): ModelAndView = {
        tableService.asyncLoadAll()

        list(0).addObject("message", "All tables will shortly be loaded from the database.")
    }

    @RequestMapping(Array("monitor/table/unload/{tableId}"))
    def unload(@PathVariable("tableId") tableId: JavaBigDecimal): ModelAndView = {
        if (tableId != null) {
            tableService.asyncUnload(tableId)
            list(0).addObject("message", "A request has been sent to unload table %s".format(tableId))
        } else {
            list(0).addObject("message", "No table ID specified")
        }
    }

    @RequestMapping(Array("monitor/table/close/{tableId}"))
    def close(@PathVariable("tableId") tableId: JavaBigDecimal): ModelAndView = {
        if (tableId != null) {
            tableService.asyncCloseTable(tableId)
            list(0).addObject("message", "A request has been sent to close table %s".format(tableId))
        } else {
            list(0).addObject("message", "No table ID specified")
        }
    }

    @RequestMapping(Array("monitor/table/reset/{tableId}"))
    def reset(@PathVariable("tableId") tableId: JavaBigDecimal): ModelAndView = {
        if (tableId != null) {
            tableService.asyncReset(tableId)
            list(0).addObject("message", "A request has been sent to reset table %s".format(tableId))
        } else {
            list(0).addObject("message", "No table ID specified")
        }
    }

    @RequestMapping(Array("monitor/table/reopen/{tableId}"))
    def reopen(@PathVariable("tableId") tableId: JavaBigDecimal): ModelAndView = {
        if (tableId != null) {
            tableService.asyncReOpen(tableId)
            list(0).addObject("message", "A request has been sent to reopen table %s".format(tableId))
        } else {
            list(0).addObject("message", "No table ID specified")
        }
    }

    @RequestMapping(Array("monitor/table/dump/{tableId}"))
    def dumpGame(@PathVariable("tableId") tableId: JavaBigDecimal,
                 response: HttpServletResponse) {
        val dump = dumpGameFromTable(tableId)
        if (dump != null) {
            response.setContentType("text/xml")
            response.getWriter.write(dump)
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
        }
    }

    private def dumpGameFromTable(tableId: JavaBigDecimal): String = {
        if (tableId == null) {
            null
        } else {
            val table = tableService.findGameSummaryById(tableId)
            if (table != null) {
                table.getCurrentGameStatus
            } else {
                null
            }
        }
    }

}
