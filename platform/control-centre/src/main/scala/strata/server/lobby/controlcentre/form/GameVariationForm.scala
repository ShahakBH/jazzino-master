package strata.server.lobby.controlcentre.form

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import java.util.{List => JavaList, ArrayList => JavaArrayList}
import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import strata.server.lobby.controlcentre.model.GameVariation

class GameVariationForm(@BeanProperty var id: BigDecimal,
                        @BeanProperty var gameType: String,
                        @BeanProperty var name: String,
                        @BeanProperty var properties: JavaList[GameVariationPropertyForm]) {

    def this() {
        this (null, null, null, new JavaArrayList[GameVariationPropertyForm]())
    }

    def this(gameVariation: GameVariation) {
        this (gameVariation.id, gameVariation.gameType,
            gameVariation.name, gameVariation.properties.map { new GameVariationPropertyForm(_) })
    }

    def toGameVariation: GameVariation =
            new GameVariation(id, gameType, name, properties.map({ _.toGameVariationProperty }).toList)

    override def toString: String = new ToStringBuilder(this)
            .append(id)
            .append(gameType)
            .append(name)
            .append(properties)
            .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: GameVariationForm => other.getClass == getClass &&
                new EqualsBuilder()
                        .append(id, other.id)
                        .append(gameType, other.gameType)
                        .append(name, other.name)
                        .append(properties, other.properties)
                        .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
            .append(id)
            .append(gameType)
            .append(name)
            .append(properties)
            .hashCode

}
