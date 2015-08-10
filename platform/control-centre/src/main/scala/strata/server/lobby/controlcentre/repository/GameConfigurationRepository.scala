package strata.server.lobby.controlcentre.repository

import com.yazino.platform.table.GameConfiguration
import java.math.BigDecimal
import java.util

trait GameConfigurationRepository {
  def findGameById(id: String): Option[GameConfiguration]

  def findAllGames(): util.List[GameConfiguration]

  def save(configuration: GameConfiguration) : GameConfiguration

  def delete(gameId: String): Unit

  def deleteProperty(gameId: String, propertyId: BigDecimal): Unit

}
