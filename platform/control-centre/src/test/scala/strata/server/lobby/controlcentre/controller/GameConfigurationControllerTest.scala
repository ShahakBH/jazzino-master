package strata.server.lobby.controlcentre.controller

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FlatSpec}
import scala.collection.JavaConversions._
import org.mockito.Matchers.any
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import strata.server.lobby.controlcentre.repository.JDBCGameConfigurationRepository
import strata.server.lobby.controlcentre.form.{GameConfigurationPropertyForm, GameConfigurationForm}
import com.yazino.platform.table.{GameConfiguration, GameConfigurationProperty, TableConfigurationUpdateService}
import org.mockito.Mockito._
import java.util.{Arrays, List => JavaList, ArrayList => JavaArrayList}
import java.math.BigDecimal
import java.util

@RunWith(classOf[JUnitRunner])
class GameConfigurationControllerTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

  private val gameConfigurationRepository = mock[JDBCGameConfigurationRepository]
  private val tableConfigurationUpdateService = mock[TableConfigurationUpdateService]
  private val response = mock[HttpServletResponse]
  private val request = mock[HttpServletRequest]
  private val gameConfigurationProperties = {
    val property1 = new GameConfigurationProperty(BigDecimal.valueOf(1), "GAME_TYPE", "first", "value")
    val property2 = new GameConfigurationProperty(BigDecimal.valueOf(2), "GAME_TYPE", "second", "value")
    val property3 = new GameConfigurationProperty(BigDecimal.valueOf(3), "GAME_TYPE", "third", "value")
    util.Arrays.asList(property1, property2, property3)
  }

  when(request.getContextPath).thenReturn("aContextPath")

  private val underTest = new GameConfigurationController(
    gameConfigurationRepository, tableConfigurationUpdateService)

  "The Controller" should "show the requested configuration" in {
    val gameId = "BLACKJACK"
    when(gameConfigurationRepository.findGameById(gameId)).thenReturn(Some(aGameConfigurationWithProperties(gameId)))

    val modelAndView = underTest.show("BLACKJACK", response)

    modelAndView.getModel.get("gameConfiguration") should equal(configurationFormFor(aGameConfigurationWithProperties(gameId)))
  }

  it should "show all the game configurations" in {
    val game145 = "BLACKJACK"
    val game200 = "SLOTS"
    val games: util.List[GameConfiguration] = new util.ArrayList(util.Arrays.asList(aGameConfigurationWithProperties(game145), aGameConfigurationWithProperties(game200)))
    when(gameConfigurationRepository.findAllGames()).thenReturn(games)

    val modelAndView = underTest.list()

    modelAndView.getModel.get("gameConfigurations") should equal (games)
  }

  it should "return a 404 when showing a non-existent configruation" in {
    when(gameConfigurationRepository.findGameById("BLACKJACK")).thenReturn(None)

    val modelAndView = underTest.show("BLACKJACK", response)

    verify(response).sendError(404)
    modelAndView should equal(null)
  }

  it should "return the show view for the show action" in {
    when(gameConfigurationRepository.findGameById("BLACKJACK")).thenReturn(Some(aGameConfigurationWithProperties()))

    underTest.show("BLACKJACK", response).getViewName should equal("game/configuration/show")
  }

  it should "return the requested configuration for the edit action" in {
    val gameId = "BLACKJACK"
    when(gameConfigurationRepository.findGameById(gameId)).thenReturn(Some(aGameConfigurationWithProperties(gameId)))

    val modelAndView = underTest.edit("BLACKJACK", response)

    modelAndView.getModel.get("gameConfiguration") should equal(configurationFormFor(aGameConfigurationWithProperties(gameId)))
  }

  it should "return an empty property form" in {
    reset(gameConfigurationRepository)
    val gameId = "BLACKJACK"
    when(gameConfigurationRepository.findGameById(gameId)).thenReturn(Some(aGameConfigurationWithProperties(gameId)))

    val modelAndView = underTest.edit("BLACKJACK", response)

    val gameConfiguration: GameConfiguration = aGameConfigurationWithProperties(gameId)
    modelAndView.getModel.get("gameConfiguration") should equal(configurationFormFor(gameConfiguration))
    modelAndView.getModel.get("gameConfigurationProperty") should equal(new GameConfigurationPropertyForm())
  }

  it should "return a 404 when editing a non-existent game Id" in {
    when(gameConfigurationRepository.findGameById("BLACKJACK")).thenReturn(None)

    val modelAndView = underTest.edit("BLACKJACK", response)

    verify(response).sendError(404)
    modelAndView should equal(null)
  }

  it should "return the edit view for the edit action" in {
    when(gameConfigurationRepository.findGameById("BLACKJACK")).thenReturn(Some(aGameConfigurationWithProperties("BLACKJACK")))

    underTest.edit("BLACKJACK", response).getViewName should equal("game/configuration/edit")
  }

  it should "redirect to the show view after a successful save" in {
    var config = new GameConfiguration("SLOTS", "slots", "Slots", new util.ArrayList[String](), 0)
    when(gameConfigurationRepository.save(any())).thenReturn(config)

    val modelAndView = underTest.save(configurationFormFor(config), request)

    modelAndView.getViewName should equal("redirect:/game/configuration/show/SLOTS")
  }

  it should "save the configuration to the repository for the save action" in {
    var config = new GameConfiguration("SLOTS", "slots", "Slots", new util.ArrayList[String](), 0)
    when(gameConfigurationRepository.save(any())).thenReturn(config)

    underTest.save(configurationFormFor(config), request)

    verify(gameConfigurationRepository).save(config)
  }

  it should "redirect to the game configuration after creation" in {
    var config = new GameConfiguration("SLOTS", "slots", "Slots", new util.ArrayList[String](), 0)
    when(gameConfigurationRepository.save(any())).thenReturn(config)

    val modelAndView = underTest.save(configurationFormFor(config), request)

    modelAndView.getViewName should equal("redirect:/game/configuration/show/SLOTS")
  }

  it should "delete a configuration for the delete action" in {
    underTest.delete("GAME_TYPE")

    verify(gameConfigurationRepository).delete("GAME_TYPE")
  }

  it should "add a property for the add action" in {
    val gameId = "GAME_TYPE"
    var gameConfiguration = aGameConfigurationWithProperties(gameId)
    val property = aGameConfigurationProperty(BigDecimal.valueOf(5), gameId)
    when(gameConfigurationRepository.findGameById(gameId)).thenReturn(Some(gameConfiguration)) // original game configuration
    val updatedProperties = new util.ArrayList[GameConfigurationProperty](gameConfiguration.getProperties)
    updatedProperties.add(property)
    gameConfiguration = gameConfiguration.withProperties(updatedProperties)

    when(gameConfigurationRepository.save(any())).thenReturn(gameConfiguration) // with additional property
    underTest.addProperty(configurationFormFor(gameConfiguration), propertyFormFor(property), response)

    verify(gameConfigurationRepository).save(gameConfiguration)
  }

  it should "delete a property for the delete property action" in {
    when(gameConfigurationRepository.findGameById("GAME_TYPE")).thenReturn(Some(aGameConfigurationWithProperties()))

    underTest.deleteProperty(1, configurationFormFor(aGameConfigurationWithProperties()), response)

    verify(gameConfigurationRepository).deleteProperty("GAME_TYPE", BigDecimal.valueOf(1))
  }

  it should "redirect to the list view after the delete action" in {
    val modelAndView = underTest.delete("GAME_TYPE")

    modelAndView.getViewName should equal("redirect:/game/configuration/list")
  }

  it should "redirect to the list view after the delete property action" in {
    when(gameConfigurationRepository.findGameById("BLACKJACK")).thenReturn(Some(aGameConfigurationWithProperties("BLACKJACK")))
    val modelAndView = underTest.deleteProperty(15, configurationFormFor(aGameConfigurationWithProperties("BLACKJACK")), response)

    modelAndView.getViewName should equal("game/configuration/edit")
  }

  it should "convert the display name to camel case" in {
    val displayName = "Game Example"

    underTest.convertToCamelCase(displayName) should equal("gameExample")
  }

  private def configurationFormFor(gameConfiguration: GameConfiguration) = new GameConfigurationForm(gameConfiguration)

  private def propertyFormFor(gameConfiguration: GameConfigurationProperty) = new GameConfigurationPropertyForm(gameConfiguration)

  private def aGameConfigurationWithProperties(id: String = "GAME_TYPE") = {
    new GameConfiguration(id, "SLOTS", "slots", new util.ArrayList[String](), 0).withProperties(gameConfigurationProperties)
  }

  private def aGameConfigurationProperty(id: BigDecimal = BigDecimal.valueOf(10), gameId: String = "GAME_TYPE") = {
    new GameConfigurationProperty(id, gameId, "PROPERTY_NAME", "value")
  }
}
