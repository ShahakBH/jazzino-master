package strata.server.lobby.controlcentre.controller

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import javax.servlet.http.HttpServletResponse
import strata.server.lobby.controlcentre.repository.{JDBCTournamentRepository, JDBCTournamentVariationRepository}
import strata.server.lobby.controlcentre.validation.TournamentValidator
import org.springframework.validation.BindingResult
import com.yazino.platform.tournament.{TournamentType, TournamentStatus, TournamentService}
import org.junit.{After, Before, Test}
import com.yazino.platform.model.PagedData
import strata.server.lobby.controlcentre.model.{Allocator, TournamentVariation, Tournament}
import org.mockito.Mockito._
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import strata.server.lobby.controlcentre.form.TournamentForm
import java.{util, math}
import com.yazino.test.ThreadLocalDateTimeUtils

class TournamentControllerTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val response = mock[HttpServletResponse]
    private val tournamentRepository = mock[JDBCTournamentRepository]
    private val tournamentVariationRepository = mock[JDBCTournamentVariationRepository]
    private val tournamentValidator = mock[TournamentValidator]
    private val tournamentService = mock[TournamentService]
    private val bindingResult = mock[BindingResult]

    private val underTest = new TournamentController(
        tournamentRepository, tournamentVariationRepository, tournamentService, tournamentValidator)

    @Before def lockSystemTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 10, 21, 0, 0, 0, 0).getMillis)
    }

    @After def unlockSystemTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem()
    }

    @Test def variationsArePopulatedInTheModel() {
        when(tournamentVariationRepository.list()).thenReturn(Map(
            BigDecimal(10) -> "variation1", BigDecimal(20) -> "variation2"))
        val expectedVariations = new util.TreeMap[math.BigDecimal, String]()
        expectedVariations.put(BigDecimal(10).underlying(), "variation1")
        expectedVariations.put(BigDecimal(20).underlying(), "variation2")

        val variations = underTest.variations

        variations should equal(expectedVariations)
    }

    @Test def listingAtPageReturnsTheAppropriatePage() {
        val expectedData = new PagedData[Tournament](0, 2, 2, List(aTournament(1), aTournament(2)))
        when(tournamentRepository.findAll(3, 20)).thenReturn(expectedData)

        underTest.listAtPage(4)

        verify(tournamentRepository).findAll(3, 20)
    }

    @Test def listingIncludesTheCannotCancelStatuses() {
        when(tournamentRepository.findAll(3, 20)).thenReturn(
            new PagedData[Tournament](0, 2, 2, List(aTournament(1), aTournament(2))))

        val model = underTest.listAtPage(4)

        model.getModel.get("cannotCancelStatuses") should equal(TournamentForm.cannotCancelStatuses)
    }

    @Test def listingAtPageReturnsTournamentsInTheModel() {
        val expectedData = new PagedData[Tournament](0, 2, 2, List(aTournament(1), aTournament(2)))
        when(tournamentRepository.findAll(3, 20)).thenReturn(expectedData)

        val model = underTest.listAtPage(4)

        model.getModel.get("tournaments") should equal(expectedData)
    }

    @Test def listingUsesTheListView() {
        when(tournamentRepository.findAll(0, 20)).thenReturn(
            new PagedData[Tournament](0, 2, 2, List(aTournament(1), aTournament(2))))

        val model = underTest.list

        model.getViewName should equal("tournament/tournament/list")
    }

    @Test def showingANonExistentTournamentReturnsAFileNotFoundStatus() {
        when(tournamentRepository.findById(BigDecimal(100))).thenReturn(None)

        val model = underTest.show(100, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def showingAnExistingTournamentReturnsTheTournament() {
        when(tournamentRepository.findById(101)).thenReturn(Some(aTournament(101)))

        val model = underTest.show(101, response)

        model.getModel.get("tournament") should equal(new TournamentForm(aTournament(101)))
        verifyZeroInteractions(response)
    }

    @Test def showingAnExistingTournamentUsesTheShowView() {
        when(tournamentRepository.findById(101)).thenReturn(Some(aTournament(101)))

        val model = underTest.show(101, response)

        model.getViewName should equal("tournament/tournament/show")
    }

    @Test def creatingATournamentUsesTheCreateView() {
        val model = underTest.create()

        model.getViewName should equal("tournament/tournament/create")
    }

    @Test def creatingATournamentAddsAnEmptyFormToTheModel() {
        val model = underTest.create()

        model.getModel.get("tournament") should equal(new TournamentForm())
    }

    @Test def cancellingATournamentCallsCancelOnTheTournamentService() {
        underTest.cancel(101)

        verify(tournamentService).cancelTournament(BigDecimal(101).underlying())
    }

    @Test def cancellingATournamentPlacesASuccessMessageInTheModelOnSuccess() {
        when(tournamentService.cancelTournament(BigDecimal(101).underlying()))
            .thenReturn(true)

        val model = underTest.cancel(101)

        model.getModel.get("message") should equal("Tournament 101 has been cancelled.")
    }

    @Test def cancellingATournamentPlacesAFailureMessageInTheModelOnFailure() {
        when(tournamentService.cancelTournament(BigDecimal(101).underlying()))
            .thenReturn(false)

        val model = underTest.cancel(101)

        model.getModel.get("message") should equal("Tournament 101 has not been cancelled.")
    }

    @Test def savingATournamentRedirectsTheUserToTheShowView() {
        val form = new TournamentForm(aTournament(-1))
        when(tournamentVariationRepository.findById(BigDecimal(1))).thenReturn(Some(aTournamentVariation))
        when(tournamentService.createTournament(form.toTournament.toPlatform(tournamentVariationRepository)))
            .thenReturn(BigDecimal(100).underlying())

        val model = underTest.save(form, bindingResult)

        model.getViewName should equal("redirect:/tournament/tournament/show/100")
    }

    @Test def savingANewTournamentSavesTheTournamentToTheService() {
        val form = new TournamentForm(aTournament(-1))
        when(tournamentVariationRepository.findById(BigDecimal(1))).thenReturn(Some(aTournamentVariation))
        when(tournamentService.createTournament(form.toTournament.toPlatform(tournamentVariationRepository)))
            .thenReturn(BigDecimal(100).underlying())

        underTest.save(form, bindingResult)

        verify(tournamentService).createTournament(form.toTournament.toPlatform(tournamentVariationRepository))
    }

    @Test def savingANewTournamentThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new TournamentForm(aTournament())
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(tournamentValidator).validate(form, bindingResult)
        model.getViewName should equal("tournament/tournament/create")
        model.getModel.get("tournament") should equal(form)
    }

    private def aTournament(id: Long = -1) = {
        val tournamentId = if (id >= 0) BigDecimal(id) else null
        new Tournament(tournamentId, "aName", BigDecimal(1),
            new DateTime(2012, 10, 1, 0, 0, 0, 0),
            new DateTime(2012, 10, 1, 0, 0, 0, 0),
            new DateTime(2012, 10, 1, 0, 0, 0, 0),
            TournamentStatus.ANNOUNCED, "PLAY_FOR_FUN", "aDescription")
    }

    private def aTournamentVariation =
        new TournamentVariation(BigDecimal(1), TournamentType.PRESET, "aName", BigDecimal(100),
            BigDecimal(0), BigDecimal(200), 1, 2, "BLACKJACK", 100, BigDecimal(10000),
            Allocator.EVEN_BY_BALANCE, List(), List())

}
