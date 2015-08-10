package strata.server.lobby.controlcentre.repository

import java.util.{List => JavaList, Map => JavaMap}
import scala.collection.JavaConversions._
import strata.server.lobby.controlcentre.model.GameVariationPropertyOption

class GameVariationPropertyOptionRepository(val gameVariationPropertyOptions: JavaMap[String, JavaList[GameVariationPropertyOption]]) {

    def optionsFor(gameType: String): List[GameVariationPropertyOption] =
        if (gameVariationPropertyOptions.contains(gameType)) {
            gameVariationPropertyOptions.get(gameType).toList
        } else {
            List()
        }

}
