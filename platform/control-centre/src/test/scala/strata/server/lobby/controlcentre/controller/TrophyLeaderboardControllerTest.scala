package strata.server.lobby.controlcentre.controller

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import javax.servlet.http.HttpServletResponse
import com.yazino.platform.community.{Trophy, TrophyService}
import com.yazino.platform.table.{GameTypeInformation, TableService}
import strata.server.lobby.controlcentre.validation.TrophyLeaderboardValidator
import org.springframework.validation.BindingResult
import com.yazino.platform.tournament._
import org.junit.{After, Before, Test}
import org.mockito.Mockito._
import java.{util, math}
import scala.collection.JavaConversions._
import com.yazino.platform.model.PagedData
import java.lang.Integer
import org.joda.time.{Interval, Duration, DateTime}
import strata.server.lobby.controlcentre.form.TrophyLeaderboardForm
import com.yazino.test.ThreadLocalDateTimeUtils
import com.yazino.game.api.{GameType, GameFeature}

class TrophyLeaderboardControllerTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val response = mock[HttpServletResponse]
    private val trophyService = mock[TrophyService]
    private val tableService = mock[TableService]
    private val trophyLeaderboardService = mock[TrophyLeaderboardService]
    private val trophyLeaderboardValidator = mock[TrophyLeaderboardValidator]
    private val bindingResult = mock[BindingResult]

    private val underTest = new TrophyLeaderboardController(
        trophyLeaderboardService, tableService, trophyService, trophyLeaderboardValidator)

    @Before def lockSystemTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 10, 21, 0, 0, 0, 0).getMillis)
    }

    @Before def setUp() {
        when(trophyService.findForGameType("aGameType")).thenReturn(List(
            new Trophy(BigDecimal(10).underlying(), "trophy1", "aGameType", "anImage1"),
            new Trophy(BigDecimal(20).underlying(), "trophy2", "aGameType", "anImage2")))
    }

    @After def unlockSystemTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem()
    }

    @Test def theGameTypesArePopulatedInTheModel() {
        when(tableService.getGameTypes).thenReturn(Set(
            new GameTypeInformation(new GameType("gameType1", "Game 1", Set[String](), Set(GameFeature.TOURNAMENT)), true),
            new GameTypeInformation(new GameType("gameType2", "Game 2", Set[String](), Set(GameFeature.TOURNAMENT)), true),
            new GameTypeInformation(new GameType("gameType3", "Game 3", Set[String]()), true)))

        val expectedMap = new util.HashMap[String, String]
        expectedMap.put("gameType1", "Game 1")
        expectedMap.put("gameType2", "Game 2")
        underTest.gameTypes should equal(expectedMap)
    }

    @Test def listingReturnsAllLeaderboards() {
        val expectedData = new PagedData[TrophyLeaderboardView](0, 2, 2, List(aView(1), aView(2)))
        when(trophyLeaderboardService.findAll).thenReturn(expectedData.getData)

        val model = underTest.list

        model.getModel.get("leaderboards") should equal(expectedData)
    }

    @Test def listingAtPageReturnsTheAppropriatePage() {
        var leaderboardList = List[TrophyLeaderboardView]()
        for (i <- (22.to(1, -1))) {
            leaderboardList ::= aView(i)
        }
        when(trophyLeaderboardService.findAll()).thenReturn(leaderboardList)

        val model = underTest.listAtPage(2)

        val expectedData = new PagedData[TrophyLeaderboardView](20, 2, 22, List(aView(21), aView(22)))
        model.getModel.get("leaderboards") should equal(expectedData)
    }

    @Test def listingUsesTheListView() {
        when(trophyLeaderboardService.findAll()).thenReturn(List(aView(1), aView(2)))

        val model = underTest.list

        model.getViewName should equal("tournament/leaderboard/list")
    }

    @Test def showingANonExistentLeaderboardReturnsAFileNotFoundStatus() {
        when(trophyLeaderboardService.findById(BigDecimal(100).underlying())).thenReturn(null)

        val model = underTest.show(100, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def showingAnExistingLeaderboardReturnsTheLeaderboard() {
        when(trophyLeaderboardService.findById(BigDecimal(101).underlying())).thenReturn(aView(101))

        val model = underTest.show(101, response)

        model.getModel.get("leaderboard") should equal(new TrophyLeaderboardForm(aView(101)))
        verifyZeroInteractions(response)
    }

    @Test def showingAnExistingLeaderboardUsesTheShowView() {
        when(trophyLeaderboardService.findById(BigDecimal(101).underlying())).thenReturn(aView(101))

        val model = underTest.show(101, response)

        model.getViewName should equal("tournament/leaderboard/show")
    }

    @Test def showingAnExistingLeaderboardAddsTheTrophiesToTheModel() {
        when(trophyLeaderboardService.findById(BigDecimal(101).underlying())).thenReturn(aView(101))

        val model = underTest.show(101, response)

        model.getModel.get("trophies") should equal(trophies())
    }

    @Test def creatingALeaderboardUsesTheCreateView() {
        val model = underTest.create()

        model.getViewName should equal("tournament/leaderboard/create")
    }

    @Test def creatingALeaderboardAddsAnEmptyFormToTheModel() {
        val model = underTest.create()

        model.getModel.get("leaderboard") should equal(new TrophyLeaderboardForm())
    }

    @Test def editingALeaderboardUsesTheEditView() {
        when(trophyLeaderboardService.findById(BigDecimal(101).underlying())).thenReturn(aView(101))

        val model = underTest.edit(101, response)

        model.getViewName should equal("tournament/leaderboard/edit")
    }

    @Test def editingAnLeaderboardAddsTheAppropriateFormToTheModel() {
        when(trophyLeaderboardService.findById(BigDecimal(101).underlying())).thenReturn(aView(101))

        val model = underTest.edit(101, response)

        model.getModel.get("leaderboard") should equal(new TrophyLeaderboardForm(aView(101)))
    }

    @Test def editingAnLeaderboardAddsTheTrophiesToTheModel() {
        when(trophyLeaderboardService.findById(BigDecimal(101).underlying())).thenReturn(aView(101))

        val model = underTest.edit(101, response)

        model.getModel.get("trophies") should equal(trophies())
    }

    @Test def editingANonExistentLeaderboardReturnsANotFoundError() {
        when(trophyLeaderboardService.findById(BigDecimal(101).underlying())).thenReturn(null)

        val model = underTest.edit(101, response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def disablingALeaderboardCallsTheTrophyLeaderboardService() {
        underTest.disable(101)

        verify(trophyLeaderboardService).setIsActive(BigDecimal(101).underlying(), false)
    }

    @Test def disablingALeaderboardPlacesASuccessMessageInTheModelOnSuccess() {
        val model = underTest.disable(101)

        model.getModel.get("message") should equal("Leaderboard 101 has been disabled.")
    }

    @Test def enablingALeaderboardCallsTheTrophyLeaderboardService() {
        underTest.enable(101)

        verify(trophyLeaderboardService).setIsActive(BigDecimal(101).underlying(), true)
    }

    @Test def enablingALeaderboardPlacesASuccessMessageInTheModelOnSuccess() {
        val model = underTest.enable(101)

        model.getModel.get("message") should equal("Leaderboard 101 has been enabled.")
    }

    @Test def savingALeaderboardRedirectsTheUserToTheShowView() {
        val form = new TrophyLeaderboardForm(aView(100))
        when(trophyLeaderboardService.findById(math.BigDecimal.valueOf(100))).thenReturn(aView(100))

        val model = underTest.save(form, bindingResult)

        model.getViewName should equal("redirect:/tournament/leaderboard/show/100")
    }

    @Test def savingAnExistingLeaderboardSavesTheLeaderboardToTheRepository() {
        val form = new TrophyLeaderboardForm(aView(100))
        when(trophyLeaderboardService.findById(math.BigDecimal.valueOf(100))).thenReturn(aView(100))

        underTest.save(form, bindingResult)

        verify(trophyLeaderboardService).update(aView(100))
    }

    @Test def savingAnExistingLeaderboardPreservesThePlayersAndEndPeriod() {
        val players = new TrophyLeaderboardPlayers()
        val persistedView = new TrophyLeaderboardView(math.BigDecimal.valueOf(100), "aName", true,
            "aGameType", 0, new DateTime(2012, 1, 1, 0, 0, 0, 0), new DateTime(2012, 2, 1, 0, 0, 0, 0),
            new DateTime(2012, 1, 1, 0, 0, 0, 0), new Duration(8640000),
            new util.HashMap[Integer, TrophyLeaderboardPosition](), players)
        when(trophyLeaderboardService.findById(math.BigDecimal.valueOf(100))).thenReturn(persistedView)
        val form = new TrophyLeaderboardForm(persistedView)

        underTest.save(form, bindingResult)

        verify(trophyLeaderboardService).update(persistedView)
    }

    @Test def savingANewLeaderboardSavesTheTrophyToTheRepository() {
        val form = new TrophyLeaderboardForm(aDefinition)

        underTest.save(form, bindingResult)

        verify(trophyLeaderboardService).create(form.toDefinition)
    }

    @Test def savingANewLeaderboardThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new TrophyLeaderboardForm(aDefinition)
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(trophyLeaderboardValidator).validate(form, bindingResult)
        model.getViewName should equal("tournament/leaderboard/create")
        model.getModel.get("leaderboard") should equal(form)
    }

    @Test def savingAnExistingLeaderboardThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new TrophyLeaderboardForm(aView(100))
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(trophyLeaderboardValidator).validate(form, bindingResult)
        model.getViewName should equal("tournament/leaderboard/edit")
        model.getModel.get("leaderboard") should equal(form)
    }

    private def trophies() = {
        val expectedMap = new util.HashMap[math.BigDecimal, String]
        expectedMap.put(math.BigDecimal.valueOf(-1), "None")
        expectedMap.put(math.BigDecimal.valueOf(10), "trophy1")
        expectedMap.put(math.BigDecimal.valueOf(20), "trophy2")
        expectedMap
    }

    private def aView(id: BigDecimal) = new TrophyLeaderboardView(id.underlying(), "aName", true,
        "aGameType", 0, new DateTime(2012, 1, 1, 0, 0, 0, 0), new DateTime(2012, 2, 1, 0, 0, 0, 0),
        new DateTime(2012, 1, 1, 0, 0, 0, 0), new Duration(8640000),
        new util.HashMap[Integer, TrophyLeaderboardPosition](), null)

    private def aDefinition = new TrophyLeaderboardDefinition("aName", "aGameType",
        new Interval(new DateTime(2012, 1, 1, 0, 0, 0, 0), new DateTime(2012, 2, 1, 0, 0, 0, 0)),
        new Duration(8640000), 0, new util.HashMap[Integer, TrophyLeaderboardPosition]())

}
