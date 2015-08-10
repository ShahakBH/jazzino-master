package strata.server.lobby.controlcentre.model

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}

class GameVariation(val id: BigDecimal,
                    val gameType: String,
                    val name: String,
                    val properties: List[GameVariationProperty]) {

    def this(gameType: String,  name: String,  properties: List[GameVariationProperty]) {
        this(null, gameType, name, properties)
    }

    def withProperties(properties: List[GameVariationProperty]): GameVariation
            = new GameVariation(id, gameType, name, properties)

    def withId(id: BigDecimal): GameVariation
            = new GameVariation(id, gameType, name, properties)

    override def toString: String = new ToStringBuilder(this)
            .append(id)
            .append(gameType)
            .append(name)
            .append(properties)
            .toString

    override def equals(obj: Any): Boolean =  obj match {
        case other: GameVariation => other.getClass == getClass &&
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
