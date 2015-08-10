package strata.server.lobby.controlcentre.controller

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FlatSpec}
import org.mockito.Mockito._
import com.yazino.platform.model.PagedData
import java.util.Arrays.asList
import java.math.{BigDecimal => JavaBigDecimal}
import com.yazino.platform.table._
import java.util.Collections
import scala.collection.JavaConversions._
import javax.servlet.http.HttpServletResponse
import java.io.{PrintWriter, ByteArrayOutputStream}
import com.yazino.game.api.GameType

class TableMonitorControllerTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

    val tableService = mock[TableService]
    val response = mock[HttpServletResponse]
    val responseOut = new ByteArrayOutputStream()

    val underTest = new TableMonitorController(tableService)

    when(response.getWriter).thenReturn(new PrintWriter(responseOut))

    "The Controller" should "show the list view for the list action" in {
        val modelAndView = underTest.list(10)

        modelAndView.getViewName should equal("monitor/table/list")
    }

    it should "fetch the given page of tables from the table service for the list action" in {
        underTest.list(14)

        verify(tableService).findByType(TableType.ALL, 14)
    }

    it should "include the found tables in the model for the list action" in {
        val expectedPagedData = new PagedData[TableSummary](10, 3, 17, asList(aTable(34)))
        when(tableService.findByType(TableType.ALL, 10)).thenReturn(expectedPagedData)

        val modelAndView = underTest.list(10)

        modelAndView.getModel.get("tables") should equal(expectedPagedData)
    }

    it should "include the available table types as a model attribute" in {
        underTest.tableTypes should equal(TableType.values())
    }

    it should "show the list view for the list at page action" in {
        val modelAndView = underTest.listAtPage(10)

        modelAndView.getViewName should equal("monitor/table/list")
    }

    it should "fetch the given page of tables from the table service for the list at page action" in {
        underTest.listAtPage(4)

        verify(tableService).findByType(TableType.ALL, 3)
    }

    it should "include the found tables in the model for the list at page action" in {
        val expectedPagedData = new PagedData[TableSummary](9, 3, 17, asList(aTable(34)))
        when(tableService.findByType(TableType.ALL, 9)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPage(10)

        modelAndView.getModel.get("tables") should equal(expectedPagedData)
    }

    it should "show the list view for the list at page with status action" in {
        val modelAndView = underTest.listAtPageWithOptions("all", 10, false, null)

        modelAndView.getViewName should equal("monitor/table/list")
    }

    it should "fetch the given page of tables with the minimum required options from the table service for the list at page with options action" in {
        underTest.listAtPageWithOptions("private", 4, false, "")

        verify(tableService).findByType(TableType.PRIVATE, 3)
    }

    it should "fetch the given page of tables with the optional options from the table service for the list at page with options action" in {
        underTest.listAtPageWithOptions("private", 4, true, "error")

        verify(tableService).findByType(TableType.PRIVATE, 3,
            List(TableSearchOption.ONLY_WITH_PLAYERS, TableSearchOption.IN_ERROR_STATE): _*)
    }

    it should "include the found tables in the model for the list at page with options action" in {
        val expectedPagedData = new PagedData[TableSummary](9, 3, 17, asList(aTable(34)))
        when(tableService.findByType(TableType.TOURNAMENT, 9, List(TableSearchOption.ONLY_OPEN): _*)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPageWithOptions("tournament", 10, false, "open")

        modelAndView.getModel.get("tables") should equal(expectedPagedData)
    }

    it should "include the table type in the model for the list at page with options action" in {
        val expectedPagedData = new PagedData[TableSummary](9, 3, 17, asList(aTable(34)))
        when(tableService.findByType(TableType.PRIVATE, 9)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPageWithOptions("private", 10, false, null)

        modelAndView.getModel.get("tableType") should equal(TableType.PRIVATE)
    }

    it should "include the options in the model for the list at page with options action" in {
        val expectedPagedData = new PagedData[TableSummary](9, 3, 17, asList(aTable(34)))
        val expectedOptions = List(TableSearchOption.IN_ERROR_STATE)
        when(tableService.findByType(TableType.PRIVATE, 9, expectedOptions: _*)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPageWithOptions("private", 10, false, "error")

        modelAndView.getModel.get("showState") should equal("error")
        modelAndView.getModel.get("onlyWithPlayers") should equal(false)
    }

    it should "load all tables for the load all action" in {
        underTest.loadAll()

        verify(tableService).asyncLoadAll()
    }

    it should "add a completion message to the model for the load all action" in {
        val modelAndView = underTest.loadAll()

        modelAndView.getModel.get("message") should equal("All tables will shortly be loaded from the database.")
    }

    it should "unload the given table for the unload action" in {
        val tableId = new JavaBigDecimal(321)
        underTest.unload(tableId)

        verify(tableService).asyncUnload(tableId)
    }

    it should "return the list of tables for the unload action" in {
        val expectedPagedData = new PagedData[TableSummary](10, 3, 17, asList(aTable(34)))
        when(tableService.findByType(TableType.ALL, 0)).thenReturn(expectedPagedData)

        val modelAndView = underTest.unload(new JavaBigDecimal(34))

        modelAndView.getModel.get("tables") should equal(expectedPagedData)
        modelAndView.getViewName should equal("monitor/table/list")
    }

    it should "add a completion message to the model for the unload action" in {
        val tableId = new JavaBigDecimal(321)
        val modelAndView = underTest.unload(tableId)

        modelAndView.getModel.get("message") should equal("A request has been sent to unload table %s".format(tableId))
    }

    it should "add an error message to the model if the table ID is null for the unload action" in {
        val modelAndView = underTest.unload(null)

        modelAndView.getModel.get("message") should equal("No table ID specified")
    }

    it should "take no actions if the table ID is null for the unload action" in {
        underTest.unload(null)

        verify(tableService).findByType(TableType.ALL, 0)
        verifyNoMoreInteractions(tableService)
    }

    it should "close the given table for the close action" in {
        val tableId = new JavaBigDecimal(321)
        underTest.close(tableId)

        verify(tableService).asyncCloseTable(tableId)
    }

    it should "return the list of tables for the close action" in {
        val expectedPagedData = new PagedData[TableSummary](10, 3, 17, asList(aTable(34)))
        when(tableService.findByType(TableType.ALL, 0)).thenReturn(expectedPagedData)

        val modelAndView = underTest.close(new JavaBigDecimal(34))

        modelAndView.getModel.get("tables") should equal(expectedPagedData)
        modelAndView.getViewName should equal("monitor/table/list")
    }

    it should "add a completion message to the model for the close action" in {
        val tableId = new JavaBigDecimal(321)
        val modelAndView = underTest.close(tableId)

        modelAndView.getModel.get("message") should equal("A request has been sent to close table %s".format(tableId))
    }

    it should "add an error message to the model if the table ID is null for the close action" in {
        val modelAndView = underTest.close(null)

        modelAndView.getModel.get("message") should equal("No table ID specified")
    }

    it should "take no actions if the table ID is null for the close action" in {
        underTest.close(null)

        verify(tableService).findByType(TableType.ALL, 0)
        verifyNoMoreInteractions(tableService)
    }

    it should "reset the given table for the reset action" in {
        val tableId = new JavaBigDecimal(321)
        underTest.reset(tableId)

        verify(tableService).asyncReset(tableId)
    }

    it should "return the list of tables for the reset action" in {
        val expectedPagedData = new PagedData[TableSummary](10, 3, 17, asList(aTable(34)))
        when(tableService.findByType(TableType.ALL, 0)).thenReturn(expectedPagedData)

        val modelAndView = underTest.reset(new JavaBigDecimal(34))

        modelAndView.getModel.get("tables") should equal(expectedPagedData)
        modelAndView.getViewName should equal("monitor/table/list")
    }

    it should "add a completion message to the model for the reset action" in {
        val tableId = new JavaBigDecimal(321)
        val modelAndView = underTest.reset(tableId)

        modelAndView.getModel.get("message") should equal("A request has been sent to reset table %s".format(tableId))
    }

    it should "add an error message to the model if the table ID is null for the reset action" in {
        val modelAndView = underTest.reset(null)

        modelAndView.getModel.get("message") should equal("No table ID specified")
    }

    it should "take no actions if the table ID is null for the reset action" in {
        underTest.reset(null)

        verify(tableService).findByType(TableType.ALL, 0)
        verifyNoMoreInteractions(tableService)
    }

    it should "reopen the given table for the reopen action" in {
        val tableId = new JavaBigDecimal(321)
        underTest.reopen(tableId)

        verify(tableService).asyncReOpen(tableId)
    }

    it should "return the list of tables for the reopen action" in {
        val expectedPagedData = new PagedData[TableSummary](10, 3, 17, asList(aTable(34)))
        when(tableService.findByType(TableType.ALL, 0)).thenReturn(expectedPagedData)

        val modelAndView = underTest.reopen(new JavaBigDecimal(34))

        modelAndView.getModel.get("tables") should equal(expectedPagedData)
        modelAndView.getViewName should equal("monitor/table/list")
    }

    it should "add a completion message to the model for the reopen action" in {
        val tableId = new JavaBigDecimal(321)
        val modelAndView = underTest.reopen(tableId)

        modelAndView.getModel.get("message") should equal("A request has been sent to reopen table %s".format(tableId))
    }

    it should "add an error message to the model if the table ID is null for the reopen action" in {
        val modelAndView = underTest.reopen(null)

        modelAndView.getModel.get("message") should equal("No table ID specified")
    }

    it should "take no actions if the table ID is null for the reopen action" in {
        underTest.reopen(null)

        verify(tableService).findByType(TableType.ALL, 0)
        verifyNoMoreInteractions(tableService)
    }

    it should "send a 404 status if the table ID is null for the dump action" in {
        underTest.dumpGame(null, response)

        verify(response).sendError(404)
    }

    it should "set the content type to XML for the dump action" in {
        val tableId = new JavaBigDecimal(342)
        when(tableService.findGameSummaryById(tableId)).thenReturn(aGame())

        underTest.dumpGame(tableId, response)

        verify(response).setContentType("text/xml")
    }

    it should "write the XML table dump to the response for the dump action" in {
        val tableId = new JavaBigDecimal(342)
        when(tableService.findGameSummaryById(tableId)).thenReturn(aGame())

        underTest.dumpGame(tableId, response)

        response.getWriter.flush()
        responseOut.toString should equal(
            "<strata.server.lobby.controlcentre.controller.TableMonitorControllerTest_-_-anon_-1/>")
    }

    private def aGame() =
        new TableGameSummary(new JavaBigDecimal(100), "aName", 200, aGameType(), aGameStatus, 300)

    private def aGameStatus() = "<gameStatus><aGameStatus/></gameStatus>"

    private def aTable(id: Long) =
        new TableSummary(new JavaBigDecimal(id), "aTable", TableStatus.open, "aGameTypeId", aGameType(),
            new JavaBigDecimal(10), "aClientId", "aClientFile", "aTemplate", "aMonitoringMessage",
            Set(new JavaBigDecimal(2)), Collections.emptySet())

    private def aGameType() = new GameType("aGame", "a Game", Collections.emptySet())
}
