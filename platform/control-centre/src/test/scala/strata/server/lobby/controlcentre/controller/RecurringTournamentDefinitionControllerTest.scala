package strata.server.lobby.controlcentre.controller

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import javax.servlet.http.HttpServletResponse
import strata.server.lobby.controlcentre.repository.{JDBCTournamentVariationRepository, JDBCRecurringTournamentDefinitionRepository}
import org.springframework.validation.BindingResult
import strata.server.lobby.controlcentre.validation.RecurringTournamentDefinitionValidator
import org.junit.{Before, Test}
import org.mockito.Mockito._
import com.yazino.platform.model.PagedData
import strata.server.lobby.controlcentre.model.{Allocator, TournamentVariation, RecurringTournamentDefinition}
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import strata.server.lobby.controlcentre.form.RecurringTournamentDefinitionForm
import com.yazino.platform.tournament.{TournamentType, TournamentService}

class RecurringTournamentDefinitionControllerTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val response = mock[HttpServletResponse]
    private val definitionRepository = mock[JDBCRecurringTournamentDefinitionRepository]
    private val tournamentVariationRepository = mock[JDBCTournamentVariationRepository]
    private val definitionValidator = mock[RecurringTournamentDefinitionValidator]
    private val bindingResult = mock[BindingResult]
    private val tournamentService = mock[TournamentService]

    private val underTest = new RecurringTournamentDefinitionController(
        definitionRepository, tournamentVariationRepository, tournamentService, definitionValidator)

    @Before def setUp() {
        when(tournamentVariationRepository.findById(BigDecimal(100))).thenReturn(Some(aVariation))
    }

    @Test def listingReturnsAllDefinitions() {
        val expectedData = new PagedData[RecurringTournamentDefinition](0, 2, 2, List(aDefinition(1), aDefinition(2)))
        when(definitionRepository.findAll(0, 20)).thenReturn(expectedData)

        val model = underTest.list

        model.getModel.get("definitions") should equal(expectedData)
    }

    @Test def listingAtPageReturnsTheAppropriatePage() {
        val expectedData = new PagedData[RecurringTournamentDefinition](0, 2, 2, List(aDefinition(1), aDefinition(2)))
        when(definitionRepository.findAll(3, 20)).thenReturn(expectedData)

        underTest.listAtPage(4)

        verify(definitionRepository).findAll(3, 20)
    }

    @Test def listingUsesTheListView() {
        when(definitionRepository.findAll(0, 20)).thenReturn(
            new PagedData[RecurringTournamentDefinition](0, 2, 2, List(aDefinition(1), aDefinition(2))))

        val model = underTest.list

        model.getViewName should equal("tournament/recurring/list")
    }

    @Test def showingANonExistentDefinitionReturnsAFileNotFoundStatus() {
        when(definitionRepository.findById(BigDecimal(100))).thenReturn(None)

        val model = underTest.show(100, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def showingAnExistingDefinitionReturnsTheDefinition() {
        when(definitionRepository.findById(101)).thenReturn(Some(aDefinition(101)))

        val model = underTest.show(101, response)

        model.getModel.get("definition") should equal(new RecurringTournamentDefinitionForm(aDefinition(101)))
        verifyZeroInteractions(response)
    }

    @Test def showingAnExistingDefinitionUsesTheShowView() {
        when(definitionRepository.findById(101)).thenReturn(Some(aDefinition(101)))

        val model = underTest.show(101, response)

        model.getViewName should equal("tournament/recurring/show")
    }

    @Test def creatingADefinitionUsesTheCreateView() {
        val model = underTest.create()

        model.getViewName should equal("tournament/recurring/create")
    }

    @Test def creatingADefinitionAddsAnEmptyFormToTheModel() {
        val model = underTest.create()

        model.getModel.get("definition") should equal(new RecurringTournamentDefinitionForm())
    }

    @Test def editingADefinitionUsesTheEditView() {
        when(definitionRepository.findById(101)).thenReturn(Some(aDefinition(101)))

        val model = underTest.edit(101, response)

        model.getViewName should equal("tournament/recurring/edit")
    }

    @Test def editingAnExistingDefinitionAddsTheAppropriateFormToTheModel() {
        when(definitionRepository.findById(101)).thenReturn(Some(aDefinition(101)))

        val model = underTest.edit(101, response)

        model.getModel.get("definition") should equal(new RecurringTournamentDefinitionForm(aDefinition(101)))
    }

    @Test def editingANonExistentDefinitionReturnsANotFoundError() {
        when(definitionRepository.findById(101)).thenReturn(None)

        val model = underTest.edit(101, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def savingADefinitionRedirectsTheUserToTheShowView() {
        val form = new RecurringTournamentDefinitionForm(aDefinition(100))
        when(definitionRepository.save(form.toDefinition)).thenReturn(form.toDefinition)

        val model = underTest.save(form, bindingResult)

        model.getViewName should equal("redirect:/tournament/recurring/show/100")
    }

    @Test def savingAnExistingDefinitionSavesTheDefinitionToTheService() {
        val form = new RecurringTournamentDefinitionForm(aDefinition(100))
        when(definitionRepository.save(form.toDefinition)).thenReturn(form.toDefinition)

        underTest.save(form, bindingResult)

        verify(tournamentService).saveRecurringTournamentDefinition(
            form.toDefinition.toPlatform(tournamentVariationRepository))
    }

    @Test def savingAnExistingDefinitionSavesTheDefinitionToTheRepository() {
        val form = new RecurringTournamentDefinitionForm(aDefinition(100))
        when(definitionRepository.save(form.toDefinition)).thenReturn(form.toDefinition)

        underTest.save(form, bindingResult)

        verify(definitionRepository).save(form.toDefinition)
    }

    @Test def savingANewDefinitionSavesTheDefinitionToTheRepository() {
        val form = new RecurringTournamentDefinitionForm(aDefinition(-1))
        when(definitionRepository.save(form.toDefinition)).thenReturn(form.toDefinition.withId(BigDecimal(100)))

        underTest.save(form, bindingResult)

        verify(definitionRepository).save(form.toDefinition)
    }

    @Test def savingANewDefinitionSavesTheDefinitionToTheService() {
        val form = new RecurringTournamentDefinitionForm(aDefinition(-1))
        when(definitionRepository.save(form.toDefinition)).thenReturn(form.toDefinition.withId(BigDecimal(100)))

        underTest.save(form, bindingResult)

        verify(tournamentService).saveRecurringTournamentDefinition(
            form.toDefinition.withId(BigDecimal(100)).toPlatform(tournamentVariationRepository))
    }

    @Test def savingANewDefinitionThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new RecurringTournamentDefinitionForm(aDefinition())
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(definitionValidator).validate(form, bindingResult)
        model.getViewName should equal("tournament/recurring/create")
        model.getModel.get("definition") should equal(form)
    }

    @Test def savingAnExistingDefinitionThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new RecurringTournamentDefinitionForm(aDefinition(100))
        when(bindingResult.hasErrors).thenReturn(true)
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(definitionValidator).validate(form, bindingResult)
        model.getViewName should equal("tournament/recurring/edit")
        model.getModel.get("definition") should equal(form)
    }

    private def aDefinition(id: Long = -1) = {
        val definitionId = if (id >= 0) {
            BigDecimal(id)
        } else {
            null
        }
        new RecurringTournamentDefinition(definitionId, "aTournamentName" + id,
            "aDescription", "aPartner", new DateTime(2012, 10, 1, 0, 0, 0, 0),
            300000, 3600000, BigDecimal(100), true, List())
    }

    private def aVariation =
        new TournamentVariation(BigDecimal(100), TournamentType.PRESET, "aTestTournament", BigDecimal(100),
            BigDecimal(200), BigDecimal(1000), 3, 10, "BLACKJACK", 60 * 60 * 24, BigDecimal(1500),
            Allocator.EVEN_BY_BALANCE, List(), List())

}
