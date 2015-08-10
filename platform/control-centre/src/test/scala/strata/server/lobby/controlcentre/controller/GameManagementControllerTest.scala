package strata.server.lobby.controlcentre.controller

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.yazino.platform.table.{GameTypeInformation, TableService, TableConfigurationUpdateService}
import strata.server.lobby.controlcentre.repository.GameConfigurationRepository
import com.yazino.platform.account.AccountingShutdownService
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FlatSpec}
import org.mockito.Mockito._
import java.util
import com.yazino.game.api.GameType

@RunWith(classOf[JUnitRunner])
class GameManagementControllerTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

  private val gameConfigurationRepository = mock[GameConfigurationRepository]
  private val tableConfigurationUpdateService = mock[TableConfigurationUpdateService]
  private val tableService = mock[TableService]
  private val accountingShutdownService = mock[AccountingShutdownService]

  private val underTest = new GameManagementController(gameConfigurationRepository, tableConfigurationUpdateService, tableService, accountingShutdownService)

  "The Controller" should "show the management page" in {
    val modelAndView = underTest.index()

    modelAndView.getViewName should equal("game/management/index")
  }

  it should "return the game types for index action" in {
    val gameTypeInfo = aGameTypeInfo("gameOne", isAvailable = true)
    val gameTypes = new java.util.HashSet[GameTypeInformation](java.util.Arrays.asList(gameTypeInfo))
    when(tableService.getGameTypes).thenReturn(gameTypes)

    val modelAndView = underTest.index()

    assert(modelAndView.getModel.get("games").asInstanceOf[List[GameTypeInformation]].contains(gameTypeInfo))
  }

  def aGameTypeInfo(gameId: String, isAvailable: Boolean): GameTypeInformation = {
    new GameTypeInformation(new GameType(gameId, gameId, new util.HashSet[String]()), isAvailable)
  }

}