package strata.server.lobby.controlcentre.controller

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import javax.servlet.http.HttpServletResponse
import strata.server.lobby.controlcentre.validation.TrophyValidator
import org.springframework.validation.BindingResult
import com.yazino.platform.community.{Trophy, TrophyService, CommunityConfigurationUpdateService}
import org.junit.Test
import java.util
import com.yazino.platform.model.PagedData
import org.mockito.Mockito._
import strata.server.lobby.controlcentre.form.TrophyForm
import com.yazino.platform.table.{GameTypeInformation, TableService}
import scala.collection.JavaConversions._
import com.yazino.game.api.GameType

class TrophyControllerTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val response = mock[HttpServletResponse]
    private val trophyService = mock[TrophyService]
    private val tableService = mock[TableService]
    private val trophyValidator = mock[TrophyValidator]
    private val bindingResult = mock[BindingResult]
    private val communityConfigurationUpdateService = mock[CommunityConfigurationUpdateService]

    private val underTest = new TrophyController(
        trophyService, tableService, communityConfigurationUpdateService, trophyValidator, "aContentUrl")

    @Test def theContentUrlIsPopulatedInTheModel() {
        underTest.assetUrl should equal("aContentUrl")
    }

    @Test def theGameTypesArePopulatedInTheModel() {
        when(tableService.getGameTypes).thenReturn(Set(
            new GameTypeInformation(new GameType("gameType1", "Game 1", Set[String]()), true),
            new GameTypeInformation(new GameType("gameType2", "Game 2", Set[String]()), true)))

        val expectedMap = new util.HashMap[String, String]
        expectedMap.put("gameType1", "Game 1")
        expectedMap.put("gameType2", "Game 2")
        underTest.gameTypes should equal(expectedMap)
    }

    @Test def listingReturnsAllTrophies() {
        val expectedData = new PagedData[Trophy](0, 2, 2, List(aTrophy(1), aTrophy(2)))
        when(trophyService.findAll).thenReturn(expectedData.getData)

        val model = underTest.list

        model.getModel.get("trophies") should equal(expectedData)
    }

    @Test def listingAtPageReturnsTheAppropriatePage() {
        var trophyList = List[Trophy]()
        for (i <- (22.to(1, -1))) {
            trophyList ::= aTrophy(i)
        }
        when(trophyService.findAll()).thenReturn(trophyList)

        val model = underTest.listAtPage(2)

        val expectedData = new PagedData[Trophy](20, 2, 22, List(aTrophy(21), aTrophy(22)))
        model.getModel.get("trophies") should equal(expectedData)
    }

    @Test def listingUsesTheListView() {
        when(trophyService.findAll()).thenReturn(List(aTrophy(1), aTrophy(2)))

        val model = underTest.list

        model.getViewName should equal("tournament/trophy/list")
    }

    @Test def showingANonExistentTrophyReturnsAFileNotFoundStatus() {
        when(trophyService.findById(BigDecimal(100).underlying())).thenReturn(null)

        val model = underTest.show(100, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def showingAnExistingTrophyReturnsTheTrophy() {
        when(trophyService.findById(BigDecimal(101).underlying())).thenReturn(aTrophy(101))

        val model = underTest.show(101, response)

        model.getModel.get("trophy") should equal(new TrophyForm(aTrophy(101)))
        verifyZeroInteractions(response)
    }

    @Test def showingAnExistingTrophyUsesTheShowView() {
        when(trophyService.findById(BigDecimal(101).underlying())).thenReturn(aTrophy(101))

        val model = underTest.show(101, response)

        model.getViewName should equal("tournament/trophy/show")
    }

    @Test def creatingATrophyUsesTheCreateView() {
        val model = underTest.create()

        model.getViewName should equal("tournament/trophy/create")
    }

    @Test def creatingATrophyAddsAnEmptyFormToTheModel() {
        val model = underTest.create()

        model.getModel.get("trophy") should equal(new TrophyForm())
    }

    @Test def editingATrophyUsesTheEditView() {
        when(trophyService.findById(BigDecimal(101).underlying())).thenReturn(aTrophy(101))

        val model = underTest.edit(101, response)

        model.getViewName should equal("tournament/trophy/edit")
    }

    @Test def editingAnTrophyAddsTheAppropriateFormToTheModel() {
        when(trophyService.findById(BigDecimal(101).underlying())).thenReturn(aTrophy(101))

        val model = underTest.edit(101, response)

        model.getModel.get("trophy") should equal(new TrophyForm(aTrophy(101)))
    }

    @Test def editingANonExistentTrophyReturnsANotFoundError() {
        when(trophyService.findById(BigDecimal(101).underlying())).thenReturn(null)

        val model = underTest.edit(101, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def savingATrophyRedirectsTheUserToTheShowView() {
        val form = new TrophyForm(aTrophy(100))

        val model = underTest.save(form, bindingResult)

        model.getViewName should equal("redirect:/tournament/trophy/show/100")
    }

    @Test def savingAnExistingTrophySavesTheTrophyToTheRepository() {
        val form = new TrophyForm(aTrophy(100))

        underTest.save(form, bindingResult)

        verify(trophyService).update(form.toTrophy)
    }

    @Test def savingANewTrophySavesTheTrophyToTheRepository() {
        val form = new TrophyForm(aTrophy())
        when(trophyService.create(form.toTrophy)).thenReturn(BigDecimal(10).underlying())

        underTest.save(form, bindingResult)

        verify(trophyService).create(aTrophy())
    }

    @Test def savingANewTrophyRefreshesTrophiesInTheGrid() {
        val form = new TrophyForm(aTrophy())
        when(trophyService.create(form.toTrophy)).thenReturn(BigDecimal(10).underlying())

        underTest.save(form, bindingResult)

        verify(communityConfigurationUpdateService).refreshTrophies()
    }

    @Test def savingANewTrophyThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new TrophyForm(aTrophy())
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(trophyValidator).validate(form, bindingResult)
        model.getViewName should equal("tournament/trophy/create")
        model.getModel.get("trophy") should equal(form)
    }

    @Test def savingAnExistingTrophyThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new TrophyForm(aTrophy(100))
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(trophyValidator).validate(form, bindingResult)
        model.getViewName should equal("tournament/trophy/edit")
        model.getModel.get("trophy") should equal(form)
    }

    private def aTrophy(id: Long = -1) = {
        val trophy = new Trophy()
        if (id >= 0) {
            trophy.setId(BigDecimal(id).underlying())
        }
        trophy.setName("name" + id)
        trophy.setGameType("aGameType")
        trophy.setImage("anImage")
        trophy.setMessage("aMessage")
        trophy.setShortDescription("aShortDescription")
        trophy.setMessageCabinet("aCabinetMessage")
        trophy
    }
}
