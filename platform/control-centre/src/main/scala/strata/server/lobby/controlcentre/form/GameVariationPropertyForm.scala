package strata.server.lobby.controlcentre.form

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import scala.beans.BeanProperty
import strata.server.lobby.controlcentre.model.GameVariationProperty

class GameVariationPropertyForm(@BeanProperty var id: BigDecimal,
                                @BeanProperty var name: String,
                                @BeanProperty var value: String) {

    def this() {
        this (null, null, null)
    }

    def this(name: String, value: String) {
        this (null, name, value)
    }

    def this(gameVariationProperty: GameVariationProperty) {
        this (gameVariationProperty.id, gameVariationProperty.name, gameVariationProperty.value)
    }

    def toGameVariationProperty: GameVariationProperty = new GameVariationProperty(id, name, value)

    override def toString: String = new ToStringBuilder(this)
            .append(id)
            .append(name)
            .append(value)
            .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: GameVariationPropertyForm => other.getClass == getClass &&
                new EqualsBuilder()
                        .append(id, other.id)
                        .append(name, other.name)
                        .append(value, other.value)
                        .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
            .append(id)
            .append(name)
            .append(value)
            .hashCode

}
