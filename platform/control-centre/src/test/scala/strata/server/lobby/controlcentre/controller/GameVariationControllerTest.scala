package strata.server.lobby.controlcentre.controller

import scala.collection.JavaConversions._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FlatSpec}
import org.mockito.Mockito._
import com.yazino.platform.model.PagedData
import strata.server.lobby.controlcentre.model.{GameVariationPropertyOption, GameVariationProperty, GameVariation}
import strata.server.lobby.controlcentre.repository.{GameVariationPropertyOptionRepository, JDBCGameVariationRepository}
import strata.server.lobby.controlcentre.form.GameVariationForm
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.yazino.platform.table.{TableService, GameTypeInformation, TableConfigurationUpdateService}
import java.util.Collections
import com.yazino.game.api.GameType

@RunWith(classOf[JUnitRunner])
class GameVariationControllerTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

    private val gameVariationRepository = mock[JDBCGameVariationRepository]
    private val gameVariationPropertyOptionRepository = mock[GameVariationPropertyOptionRepository]
    private val tableService = mock[TableService]
    private val tableConfigurationUpdateService = mock[TableConfigurationUpdateService]
    private val response = mock[HttpServletResponse]
    private val request = mock[HttpServletRequest]
    private val blackjackGameVariationPropertyOptions = {
        val gameVariationPropertyOption1 = new GameVariationPropertyOption("OPTION1")
        val gameVariationPropertyOption2 = new GameVariationPropertyOption("OPTION2")
        val gameVariationPropertyOption3 = new GameVariationPropertyOption("OPTION3")
        gameVariationPropertyOption2.defaultValue = "17"
        List(gameVariationPropertyOption1, gameVariationPropertyOption2, gameVariationPropertyOption3)
    }

    when(request.getContextPath).thenReturn("aContextPath")
    when(gameVariationPropertyOptionRepository.optionsFor("BLACKJACK")).thenReturn(blackjackGameVariationPropertyOptions)
    when(gameVariationPropertyOptionRepository.optionsFor("INVALID")).thenReturn(List())

    private val underTest = new GameVariationController(
        gameVariationRepository, gameVariationPropertyOptionRepository, tableService, tableConfigurationUpdateService)

    "The Controller" should "show the requested variation" in {
        when(gameVariationRepository.findById(BigDecimal("145"))).thenReturn(Some(aGameVariationWithProperties()))

        val modelAndView = underTest.show(145, response)

        modelAndView.getModel.get("gameVariation") should equal (formFor(aGameVariationWithProperties()))
    }

    it should "return a 404 when showing a non-existent variation" in {
        when(gameVariationRepository.findById(BigDecimal("145"))).thenReturn(None)

        val modelAndView = underTest.show(145, response)

        verify(response).sendError(404)
        modelAndView should equal (null)
    }

    it should "return the show view for the show action" in {
        when(gameVariationRepository.findById(BigDecimal("145"))).thenReturn(Some(aGameVariationWithProperties()))

        underTest.show(145, response).getViewName should equal ("game/variation/show")
    }

    it should "return the requested variation for the edit action" in {
        when(gameVariationRepository.findById(BigDecimal("145"))).thenReturn(Some(aGameVariationWithProperties()))

        val modelAndView = underTest.edit(145, response)

        modelAndView.getModel.get("gameVariation") should equal (formFor(aGameVariationWithProperties()))
    }

    it should "return the requested variation with any missing properties for the edit action" in {
        reset(gameVariationPropertyOptionRepository)
        when(gameVariationPropertyOptionRepository.optionsFor("BLACKJACK")).thenReturn(List(
                new GameVariationPropertyOption("OPTION1"), new GameVariationPropertyOption("OPTION2"),
                new GameVariationPropertyOption("OPTION3"), new GameVariationPropertyOption("OPTION4")))
        when(gameVariationRepository.findById(BigDecimal("145"))).thenReturn(Some(aGameVariationWithProperties()))

        val modelAndView = underTest.edit(145, response)

        val gameVariation: GameVariation = aGameVariationWithProperties()
        val expectedVariation: GameVariation = gameVariation.withProperties(
                gameVariation.properties ++ List(new GameVariationProperty(null, "OPTION4", null)))
        modelAndView.getModel.get("gameVariation") should equal (formFor(expectedVariation))
    }

    it should "return a 404 when editing a non-existent variation" in {
        when(gameVariationRepository.findById(BigDecimal("145"))).thenReturn(None)

        val modelAndView = underTest.edit(145, response)

        verify(response).sendError(404)
        modelAndView should equal (null)
    }

    it should "return the game variation options for the edit action" in {
        when(gameVariationRepository.findById(BigDecimal("145"))).thenReturn(Some(aGameVariationWithProperties()))
        val expectedOptions = Map(
                "OPTION1" -> blackjackGameVariationPropertyOptions.get(0),
                "OPTION2" -> blackjackGameVariationPropertyOptions.get(1),
                "OPTION3" -> blackjackGameVariationPropertyOptions.get(2))

        val modelAndView = underTest.edit(145, response)

        modelAndView.getModel.get("propertyOptions") should equal (expectedOptions)
    }

    it should "return the edit view for the edit action" in {
        when(gameVariationRepository.findById(BigDecimal("145"))).thenReturn(Some(aGameVariationWithProperties()))

        underTest.edit(145, response).getViewName should equal ("game/variation/edit")
    }

    it should "return the list view for the list action" in {
        when(gameVariationRepository.findAll(0, 20)).thenReturn(new PagedData[GameVariation](0, 1, 5, List(aGameVariationWithProperties())))

        underTest.list(0, 20).getViewName should equal ("game/variation/list")
    }

    it should "return the list view for the list at page action" in {
        when(gameVariationRepository.findAll(0, 20)).thenReturn(new PagedData[GameVariation](0, 1, 5, List(aGameVariationWithProperties())))

        underTest.listAtPage(1).getViewName should equal ("game/variation/list")
    }

    it should "return the paged variations when listing" in {
        val pagedData = new PagedData[GameVariation](0, 1, 5, List(aGameVariationWithProperties()))
        when(gameVariationRepository.findAll(10, 16)).thenReturn(pagedData)

        val modelAndView = underTest.list(10, 16)

        modelAndView.getModel.get("gameVariations") should equal (pagedData)
    }

    it should "return the paged variations when listing at page" in {
        val pagedData = new PagedData[GameVariation](0, 1, 5, List(aGameVariationWithProperties()))
        when(gameVariationRepository.findAll(0, 20)).thenReturn(pagedData)

        val modelAndView = underTest.listAtPage(1)

        modelAndView.getModel.get("gameVariations") should equal (pagedData)
    }

    it should "pass the given page and page size to the repository when listing" in {
        val pagedData = new PagedData[GameVariation](0, 1, 5, List(aGameVariationWithProperties()))
        when(gameVariationRepository.findAll(10, 16)).thenReturn(pagedData)

        underTest.list(10, 17)

        verify(gameVariationRepository).findAll(10, 17)
    }

    it should "pass the given page as zero-based and the default page size to the repository when listing at page" in {
        val pagedData = new PagedData[GameVariation](0, 1, 5, List(aGameVariationWithProperties()))
        when(gameVariationRepository.findAll(9, 20)).thenReturn(pagedData)

        underTest.listAtPage(10)

        verify(gameVariationRepository).findAll(9, 20)
    }

    it should "return the create view for the create action" in {
        val gameTypes = Set(new GameTypeInformation(gameTypeFor("BLACKJACK"), true), new GameTypeInformation(gameTypeFor("ROULETTE"), false))
        when(tableService.getGameTypes).thenReturn(gameTypes)

        val modelAndView = underTest.create

        modelAndView.getViewName should equal ("game/variation/create")
    }

    it should "return the available game types for the create action" in {
        val gameTypes = Set(new GameTypeInformation(gameTypeFor("BLACKJACK"), true), new GameTypeInformation(gameTypeFor("ROULETTE"), false))
        when(tableService.getGameTypes).thenReturn(gameTypes)

        val modelAndView = underTest.create

        modelAndView.getModel.get("gameTypes") should equal (List("BLACKJACK", "ROULETTE"))
    }

    it should "return a populated game variation for the create with game type action" in {
        val expectedVariation = new GameVariation(null, "BLACKJACK", "", List(
                new GameVariationProperty(null, "OPTION1", null),
                new GameVariationProperty(null, "OPTION2", "17"),
                new GameVariationProperty(null, "OPTION3", null)
        ))

        val modelAndView = underTest.createWithGameType("BLACKJACK")

        modelAndView.getModel.get("gameVariation") should equal (formFor(expectedVariation))
    }

    it should "return the game variation options for the create with game type action" in {
        val expectedOptions = Map(
                "OPTION1" -> blackjackGameVariationPropertyOptions.get(0),
                "OPTION2" -> blackjackGameVariationPropertyOptions.get(1),
                "OPTION3" -> blackjackGameVariationPropertyOptions.get(2))

        val modelAndView = underTest.createWithGameType("BLACKJACK")

        modelAndView.getModel.get("propertyOptions") should equal (expectedOptions)
    }

    it should "return no game variation property options for the create with an invalid game type action" in {
        val modelAndView = underTest.createWithGameType("INVALID")

        modelAndView.getModel.get("propertyOptions") should equal (Map())
    }

    it should "return the create view for the create with game type action" in {
        val modelAndView = underTest.createWithGameType("BLACKJACK")

        modelAndView.getViewName should equal ("game/variation/create")
    }

    it should "redirect to the show view after a successful save" in {
        when(gameVariationRepository.save(aGameVariationWithProperties())).thenReturn(aGameVariationWithProperties(BigDecimal("-1")))

        val modelAndView = underTest.save(formFor(aGameVariationWithProperties()), request)

        modelAndView.getViewName should equal ("redirect:/game/variation/show/-1")
    }

    it should "save the variation to the repository for the save action" in {
        when(gameVariationRepository.save(aGameVariationWithProperties())).thenReturn(aGameVariationWithProperties(BigDecimal("-1")))

        underTest.save(formFor(aGameVariationWithProperties()), request)

        verify(gameVariationRepository).save(aGameVariationWithProperties())
    }

    it should "refresh the templates after the save action" in {
        when(gameVariationRepository.save(aGameVariationWithProperties())).thenReturn(aGameVariationWithProperties(BigDecimal("-1")))

        underTest.save(formFor(aGameVariationWithProperties()), request)

        verify(tableConfigurationUpdateService).asyncRefreshTemplates()
    }

    it should "delete a variation for the delete action" in {
        underTest.delete(17, request)

        verify(gameVariationRepository).delete(BigDecimal("17"))
    }

    it should "redirect to the list view after the delete action" in {
        val modelAndView = underTest.delete(17, request)

        modelAndView.getViewName should equal ("redirect:/game/variation/list")
    }

    private def gameTypeFor(gameTypeId: String) = new GameType(gameTypeId, gameTypeId, Collections.emptySet())

    private def formFor(gameVariation: GameVariation) = new GameVariationForm(gameVariation)

    private def aGameVariationWithProperties(id: BigDecimal = BigDecimal(-1)) =
        new GameVariation(id, "BLACKJACK", "TEST-TEMPLATE" + id, List(
            new GameVariationProperty(id - BigDecimal(1), "OPTION1", "Value1"),
            new GameVariationProperty(id - BigDecimal(2), "OPTION2", "Value2"),
            new GameVariationProperty(id - BigDecimal(3), "OPTION3", "Value3")
        ))
}