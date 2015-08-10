package strata.server.lobby.controlcentre.controller

import scala.collection.JavaConversions._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FlatSpec}
import org.mockito.Mockito._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.yazino.platform.table.{CountdownService, TableService, GameTypeInformation, TableConfigurationUpdateService}
import java.util
import java.text.SimpleDateFormat
import com.yazino.game.api.GameType

@RunWith(classOf[JUnitRunner])
class CountdownControllerTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {
    private val DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm")
    private val ENTRY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    private val tableService = mock[TableService]
    private val tableConfigurationUpdateService = mock[TableConfigurationUpdateService]
    private val countdownService = mock[CountdownService]

    private val underTest = new CountdownController(tableConfigurationUpdateService, tableService, countdownService)

    "The Controller" should "show the countdown" in {
        val countdowns: util.Map[String, java.lang.Long] = new java.util.HashMap[String, java.lang.Long]()
        val now: util.Date = new util.Date()
        countdowns.put("aGameType", now.getTime)
        when(countdownService.findAll()).thenReturn(countdowns)

        val modelAndView = underTest.show()

        val expected = Map[String, String]().+(Pair("aGameType", DISPLAY_DATE_FORMAT.format(now)))
        modelAndView.getModel.get("countdowns") should equal(expected)
        modelAndView.getViewName should equal("maintenance/countdown/show")
    }

    it should "return a new time when creating a new countdown" in {
        val gameTypes: Set[GameTypeInformation] = util.Arrays.asList(aGameTypeInformation("BLACKJACK"), aGameTypeInformation("ROULETTE")).toSet
        when(tableService.getGameTypes).thenReturn(gameTypes)
        val modelAndView = underTest.create()

        assert(modelAndView.getModel.get("until").isInstanceOf[String])
        modelAndView.getModel.get("gameTypes") should equal(List("BLACKJACK", "ROULETTE"))
    }

    it should "return a success message when saving a countdown" in {
        val countdownInstance: util.Date = new util.Date()

        val modelAndView = underTest.save(ENTRY_DATE_FORMAT.format(countdownInstance), null)

        modelAndView.getModel.get("message") should equal("Countdown until %s started.".format(DISPLAY_DATE_FORMAT.format(countdownInstance)))
    }

    it should "return a success message when saving a countdown for a game" in {
        val countdownInstance: util.Date = new util.Date()

        val modelAndView = underTest.save(ENTRY_DATE_FORMAT.format(countdownInstance), "BLACKJACK")

        val expected: String = "Countdown for %s until %s started.".format("BLACKJACK", DISPLAY_DATE_FORMAT.format(countdownInstance))
        modelAndView.getModel.get("message") should equal(expected)
    }

    it should "parse a date with the format dd/MM/yyyy HH:mm when saving a countdown for a game" in {
        val date = DISPLAY_DATE_FORMAT.parse("07/03/2012 13:17")

        DISPLAY_DATE_FORMAT.format(date) should equal("07/03/2012 13:17")
    }

    def aGameTypeInformation(gameId: String): GameTypeInformation = {
        new GameTypeInformation(new GameType(gameId, gameId, new util.HashSet[String]()), true)
    }
}
