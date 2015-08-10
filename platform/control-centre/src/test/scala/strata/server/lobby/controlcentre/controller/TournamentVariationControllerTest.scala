package strata.server.lobby.controlcentre.controller

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import javax.servlet.http.HttpServletResponse
import strata.server.lobby.controlcentre.validation.TournamentVariationValidator
import strata.server.lobby.controlcentre.model.TournamentVariation
import strata.server.lobby.controlcentre.repository.JDBCTournamentVariationRepository
import strata.server.lobby.controlcentre.repository.mapper.GameVariationRepository
import com.yazino.platform.table.{GameClient, GameTypeInformation, TableService}
import org.junit.{Test, Before}
import org.mockito.Mockito._
import org.springframework.validation.BindingResult
import collection.JavaConversions._
import java.util
import com.yazino.platform.model.PagedData
import strata.server.lobby.controlcentre.form.TournamentVariationForm
import com.yazino.platform.tournament.TournamentType
import strata.server.lobby.controlcentre.model.Allocator
import com.yazino.game.api.{GameType, GameFeature}

class TournamentVariationControllerTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val response = mock[HttpServletResponse]
    private val bindingResult = mock[BindingResult]
    private val variationValidator = mock[TournamentVariationValidator]
    private val variationRepository = mock[JDBCTournamentVariationRepository]
    private val gameVariationRepository = mock[GameVariationRepository]
    private val tableService = mock[TableService]

    private var underTest = new TournamentVariationController(
        variationRepository, gameVariationRepository, variationValidator, tableService)

    @Before def setUp() {
        when(gameVariationRepository.listFor("BLACKJACK"))
            .thenReturn(Map(BigDecimal(1) -> "variation1", BigDecimal(2) -> "variation2"))
        when(tableService.findAllClientsFor("BLACKJACK")).thenReturn(Set(
            new GameClient("client1", 10, "aFile1", "BLACKJACK", Map[String, String]()),
            new GameClient("client2", 10, "aFile2", "BLACKJACK", Map[String, String]())))
        when(tableService.getGameTypes).thenReturn(Set(
            new GameTypeInformation(new GameType("gameType1", "Game 1", Set[String](), Set(GameFeature.TOURNAMENT)), true),
            new GameTypeInformation(new GameType("gameType2", "Game 2", Set[String](), Set(GameFeature.TOURNAMENT)), true)))
    }

    @Test def theGameTypesArePopulatedInTheModel() {
        underTest.gameTypes should equal(expectedGameTypes)
    }

    @Test def theAllocatorsArePopulatedInTheModel() {
        underTest.allocators should equal(expectedAllocators)
    }

    @Test def theExpiryDelaysArePopulatedInTheModel() {
        underTest.expiryDelays should equal(expectedDelays)
    }

    @Test def listingReturnsAllVariations() {
        val expectedData = new PagedData[TournamentVariation](0, 2, 2, List(aVariation(1), aVariation(2)))
        when(variationRepository.findAll(0, 20)).thenReturn(expectedData)

        val model = underTest.list

        model.getModel.get("variations") should equal(expectedData)
    }

    @Test def listingAPageRequestsTheAppropriatePage() {
        val expectedData = new PagedData[TournamentVariation](0, 2, 2, List(aVariation(1), aVariation(2)))
        when(variationRepository.findAll(3, 20)).thenReturn(expectedData)

        underTest.listAtPage(4)

        verify(variationRepository).findAll(3, 20)
    }

    @Test def listingUsesTheListView() {
        when(variationRepository.findAll(0, 20)).thenReturn(
            new PagedData[TournamentVariation](0, 2, 2, List(aVariation(1), aVariation(2))))

        val model = underTest.list

        model.getViewName should equal("tournament/variation/list")
    }

    @Test def showingANonExistentVariationsReturnsAFileNotFoundStatus() {
        when(variationRepository.findById(BigDecimal(100))).thenReturn(None)

        val model = underTest.show(100, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def showingAnExistingVariationReturnsTheVariation() {
        when(variationRepository.findById(101)).thenReturn(Some(aVariation(101)))

        val model = underTest.show(101, response)

        model.getModel.get("variation") should equal(new TournamentVariationForm(aVariation(101)))
        verifyZeroInteractions(response)
    }

    @Test def showingAnExistingVariationIncludesAllClientsInTheModel() {
        when(variationRepository.findById(101)).thenReturn(Some(aVariation(101)))
        val expectedClients = new util.HashMap[String, String]()
        expectedClients.put("client1", "client1")
        expectedClients.put("client2", "client2")

        val model = underTest.show(101, response)

        model.getModel.get("clients") should equal(expectedClients)
    }

    @Test def showingAnExistingVariationIncludesAllGameVariationsInTheModel() {
        when(variationRepository.findById(101)).thenReturn(Some(aVariation(101)))
        val expectedVariations = new util.HashMap[BigDecimal, String]()
        expectedVariations.put(BigDecimal(1), "variation1")
        expectedVariations.put(BigDecimal(2), "variation2")

        val model = underTest.show(101, response)

        model.getModel.get("gameVariations") should equal(expectedVariations)
    }

    @Test def showingAnExistingVariationUsesTheShowView() {
        when(variationRepository.findById(101)).thenReturn(Some(aVariation(101)))

        val model = underTest.show(101, response)

        model.getViewName should equal("tournament/variation/show")
    }

    @Test def creatingAVariationUsesTheCreateView() {
        val model = underTest.create()

        model.getViewName should equal("tournament/variation/create")
    }

    @Test def creatingAVariationAddsAnEmptyUserFormToTheModel() {
        val model = underTest.create()

        model.getModel.get("variation") should equal(new TournamentVariationForm())
    }

    @Test def editingAVariationUsesTheEditView() {
        when(variationRepository.findById(101)).thenReturn(Some(aVariation(101)))

        val model = underTest.edit(101, response)

        model.getViewName should equal("tournament/variation/edit")
    }

    @Test def editAnExistingVariationAddsTheAppropriateVariationFormToTheModel() {
        when(variationRepository.findById(101)).thenReturn(Some(aVariation(101)))

        val model = underTest.edit(101, response)

        model.getModel.get("variation") should equal(new TournamentVariationForm(aVariation(101)))
    }

    @Test def editingAnExistingVariationIncludesAllClientsInTheModel() {
        when(variationRepository.findById(101)).thenReturn(Some(aVariation(101)))

        val model = underTest.edit(101, response)

        model.getModel.get("clients") should equal(expectedClients)
    }

    @Test def editingAnExistingVariationIncludesAllGameVariationsInTheModel() {
        when(variationRepository.findById(101)).thenReturn(Some(aVariation(101)))

        val model = underTest.edit(101, response)

        model.getModel.get("gameVariations") should equal(expectedGameVariations)
    }

    @Test def editingANonExistentVariationReturnsANotFoundError() {
        when(variationRepository.findById(101)).thenReturn(None)

        val model = underTest.edit(101, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def deletingAVariationDeletesTheVariationViaTheRepository() {
        underTest.delete(101)

        verify(variationRepository).delete(BigDecimal(101))
    }

    @Test def deletingAVariationAddsAMessageToTheModel() {
        val model = underTest.delete(101)

        model.getModel.get("message") should equal("Tournament Variation 101 has been deleted.")
    }

    @Test def deletingAVariationDirectsTheUserToTheListView() {
        val expectedData = new PagedData[TournamentVariation](0, 2, 2, List(aVariation(1), aVariation(2)))
        when(variationRepository.findAll(0, 20)).thenReturn(expectedData)

        val model = underTest.delete(101)

        model.getViewName should equal("tournament/variation/list")
        model.getModel.get("variations") should equal(expectedData)
    }

    @Test def savingAVariationRedirectsTheUserToTheShowView() {
        val savedVariation = aVariation(100)
        val variationForm = new TournamentVariationForm(savedVariation)
        when(variationRepository.save(savedVariation)).thenReturn(aVariation(100))

        val model = underTest.save(variationForm, bindingResult)

        model.getViewName should equal("redirect:/tournament/variation/show/100")
    }

    @Test def savingAnExistingVariationSavesTheVariationToTheRepository() {
        val savedVariation = aVariation(100)
        val variationForm = new TournamentVariationForm(savedVariation)
        when(variationRepository.save(savedVariation)).thenReturn(aVariation(100))

        underTest.save(variationForm, bindingResult)

        verify(variationRepository).save(savedVariation)
    }

    @Test def savingANewVariationSavesTheVariationToTheRepository() {
        val savedVariation = aVariation()
        val variationForm = new TournamentVariationForm(savedVariation)
        when(variationRepository.save(savedVariation)).thenReturn(aVariation(100))

        underTest.save(variationForm, bindingResult)

        verify(variationRepository).save(savedVariation)
    }

    @Test def savingANewVariationThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val variationForm = new TournamentVariationForm(aVariation())
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(variationForm, bindingResult)

        verify(variationValidator).validate(variationForm, bindingResult)
        verifyZeroInteractions(variationRepository)
        model.getViewName should equal("tournament/variation/create")
        model.getModel.get("variation") should equal(variationForm)
    }

    @Test def savingAnExistingVariationThatFailsValidationRedirectsTheUserToTheEditPage() {
        val variationForm = new TournamentVariationForm(aVariation(100))
        when(variationRepository.save(aVariation(100))).thenReturn(aVariation(100))
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(variationForm, bindingResult)

        verify(variationValidator).validate(variationForm, bindingResult)
        verifyZeroInteractions(variationRepository)
        model.getViewName should equal("tournament/variation/edit")
        model.getModel.get("clients") should equal(expectedClients)
        model.getModel.get("gameVariations") should equal(expectedGameVariations)
    }

    private def expectedGameTypes = {
        val expectedMap = new util.TreeMap[String, String]
        expectedMap.put("gameType1", "Game 1")
        expectedMap.put("gameType2", "Game 2")
        expectedMap
    }

    private def expectedDelays: util.TreeMap[Long, String] = {
        val expectedDelays = new util.TreeMap[Long, String]()
        expectedDelays.put(86400000, "One day")
        expectedDelays.put(86400000 * 2, "Two days")
        expectedDelays.put(86400000 * 3, "Three days")
        expectedDelays.put(86400000 * 4, "Four days")
        expectedDelays.put(86400000 * 5, "Five days")
        expectedDelays.put(86400000 * 6, "Six days")
        expectedDelays.put(86400000 * 7, "Seven days")
        expectedDelays
    }

    private def expectedAllocators = {
        val expectedAllocators = new util.TreeMap[Allocator, String]()
        expectedAllocators.put(Allocator.EVEN_BY_BALANCE, Allocator.EVEN_BY_BALANCE.getDescription)
        expectedAllocators.put(Allocator.EVEN_RANDOM, Allocator.EVEN_RANDOM.getDescription)
        expectedAllocators
    }

    private def expectedClients = {
        val expectedClients = new util.TreeMap[String, String]()
        expectedClients.put("client1", "client1")
        expectedClients.put("client2", "client2")
        expectedClients
    }

    private def expectedGameVariations = {
        val expectedVariations = new util.HashMap[BigDecimal, String]()
        expectedVariations.put(BigDecimal(1), "variation1")
        expectedVariations.put(BigDecimal(2), "variation2")
        expectedVariations
    }

    private def aVariation(id: Int = -1) =
        new TournamentVariation(if (id >= 0) id else null,
            TournamentType.PRESET, "aTestTournament", BigDecimal(100),
            BigDecimal(200), BigDecimal(1000), 3, 10, "BLACKJACK", 60 * 60 * 24, BigDecimal(1500),
            Allocator.EVEN_BY_BALANCE, List(), List())

}
