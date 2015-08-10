package strata.server.lobby.controlcentre.form

import scala.beans.BeanProperty
import org.apache.commons.lang3.builder.{ToStringBuilder, HashCodeBuilder, EqualsBuilder}
import com.yazino.platform.table.GameConfigurationProperty
import java.math.BigDecimal

class GameConfigurationPropertyForm(@BeanProperty var propertyId: BigDecimal,
                                    @BeanProperty var gameId: String,
                                    @BeanProperty var propertyName: String,
                                    @BeanProperty var propertyValue: String) {

  def this() {
      this(null, null, null, null)
  }

  def this(gameConfigurationProperty: GameConfigurationProperty) {
      this(gameConfigurationProperty.getPropertyId, gameConfigurationProperty.getGameId,
          gameConfigurationProperty.getPropertyName, gameConfigurationProperty.getPropertyValue)
  }

  def toGameConfigurationProperty : GameConfigurationProperty = {
    new GameConfigurationProperty(propertyId, gameId, propertyName, propertyValue)
  }

  override def toString: String = new ToStringBuilder(this)
    .append(propertyId)
    .append(gameId)
    .append(propertyName)
    .append(propertyValue)
    .toString

  override def equals(obj: Any): Boolean = obj match {
    case other: GameConfigurationPropertyForm => other.getClass == getClass &&
      new EqualsBuilder()
        .append(propertyId, other.propertyId)
        .append(gameId, other.gameId)
        .append(propertyName, other.propertyName)
        .append(propertyValue, other.propertyValue)
        .isEquals
    case _ => false
  }

  override def hashCode: Int = new HashCodeBuilder()
    .append(propertyId)
    .append(gameId)
    .append(propertyName)
    .append(propertyValue)
    .hashCode
}
