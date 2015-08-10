package strata.server.lobby.controlcentre.controller

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FlatSpec}
import org.mockito.Mockito._
import com.yazino.platform.model.PagedData
import java.util.Arrays._
import java.math.{BigDecimal => JavaBigDecimal}
import com.yazino.platform.tournament.{TournamentService, TournamentStatus, TournamentMonitorView}

@RunWith(classOf[JUnitRunner])
class TournamentMonitorControllerTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest  {

    val tournamentService = mock[TournamentService]

    val underTest = new TournamentMonitorController(tournamentService)

    "The Controller" should "show the list view for the list action" in {
        val modelAndView = underTest.list(10)

        modelAndView.getViewName should equal("monitor/tournament/list")
    }

    it should "fetch the given page of tournaments from the tournament service for the list action" in {
        underTest.list(14)

        verify(tournamentService).findAll(14)
    }

    it should "include the found tournaments in the model for the list action" in {
        val expectedPagedData = new PagedData[TournamentMonitorView](10, 3, 17, asList(aTournament(34)))
        when(tournamentService.findAll(10)).thenReturn(expectedPagedData)

        val modelAndView = underTest.list(10)

        modelAndView.getModel.get("tournaments") should equal(expectedPagedData)
    }

    it should "include the available tournament statuses in the model for the list action" in {
        val modelAndView = underTest.list(1)

        modelAndView.getModel.get("searchStatuses") should equal(TournamentStatus.values())
    }

    it should "show the list view for the list at page action" in {
        val modelAndView = underTest.listAtPage(10)

        modelAndView.getViewName should equal("monitor/tournament/list")
    }

    it should "fetch the given page of tournaments from the tournament service for the list at page action" in {
        underTest.listAtPage(4)

        verify(tournamentService).findAll(3)
    }

    it should "include the found tournaments in the model for the list at page action" in {
        val expectedPagedData = new PagedData[TournamentMonitorView](9, 3, 17, asList(aTournament(34)))
        when(tournamentService.findAll(9)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPage(10)

        modelAndView.getModel.get("tournaments") should equal(expectedPagedData)
    }

    it should "show the list view for the list at page with status action" in {
        val modelAndView = underTest.listAtPageWithStatus("error", 10)

        modelAndView.getViewName should equal("monitor/tournament/list")
    }

    it should "fetch the given page of tournaments with the given status from the tournament service for the list at page with status action" in {
        underTest.listAtPageWithStatus("announced", 4)

        verify(tournamentService).findByStatus(TournamentStatus.ANNOUNCED, 3)
    }

    it should "include the found tournaments in the model for the list at page with status action" in {
        val expectedPagedData = new PagedData[TournamentMonitorView](9, 3, 17, asList(aTournament(34)))
        when(tournamentService.findByStatus(TournamentStatus.ERROR, 9)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPageWithStatus("ERROR", 10)

        modelAndView.getModel.get("tournaments") should equal(expectedPagedData)
    }

    it should "include the filters status in the model for the list at page with status action" in {
        val expectedPagedData = new PagedData[TournamentMonitorView](9, 3, 17, asList(aTournament(34)))
        when(tournamentService.findByStatus(TournamentStatus.ERROR, 9)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPageWithStatus("ERROR", 10)

        modelAndView.getModel.get("status") should equal(TournamentStatus.ERROR)
    }

    it should "include the available tournament statuses in the model for the list at page with status action" in {
        val expectedPagedData = new PagedData[TournamentMonitorView](9, 3, 17, asList(aTournament(34)))
        when(tournamentService.findByStatus(TournamentStatus.ERROR, 9)).thenReturn(expectedPagedData)

        val modelAndView = underTest.listAtPageWithStatus("ERROR", 10)

        modelAndView.getModel.get("searchStatuses") should equal(TournamentStatus.values())
    }

    private def aTournament(id: Long) =
        new TournamentMonitorView(new JavaBigDecimal(id), "aTournament", "aGameType",
            "aTemplate", TournamentStatus.ANNOUNCED, "aMonitoringMessage")
}