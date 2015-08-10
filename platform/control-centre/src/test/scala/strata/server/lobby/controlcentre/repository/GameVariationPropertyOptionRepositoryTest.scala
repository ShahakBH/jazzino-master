package strata.server.lobby.controlcentre.repository

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import strata.server.lobby.controlcentre.model.GameVariationPropertyOption
import java.util.{Arrays, List => JavaList, HashMap => JavaHashMap}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GameVariationPropertyOptionRepositoryTest extends FlatSpec with ShouldMatchers {

    val gameVariationPropertyOption1 = new GameVariationPropertyOption("OPTION1")
    val gameVariationPropertyOption2 = {
        val option = new GameVariationPropertyOption("OPTION2")
        option.defaultValue = "17"
        option
    }
    val options = {
        val gameTypesToOptions = new JavaHashMap[String, JavaList[GameVariationPropertyOption]]()
        gameTypesToOptions.put("BLACKJACK", Arrays.asList(gameVariationPropertyOption1, gameVariationPropertyOption2))
        gameTypesToOptions
    }
    val underTest = new GameVariationPropertyOptionRepository(options)

    "The Repository" should "return the options for the given game type" in {
        underTest.optionsFor("BLACKJACK") should equal (List(
            gameVariationPropertyOption1, gameVariationPropertyOption2))
    }

    "The Repository" should "return an empty list for an invalid game type" in {
        underTest.optionsFor("INVALID") should equal (List())
    }
}
