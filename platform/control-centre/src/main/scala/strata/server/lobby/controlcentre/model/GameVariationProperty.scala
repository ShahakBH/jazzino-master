package strata.server.lobby.controlcentre.model

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}

class GameVariationProperty(val id: BigDecimal,
                            val name: String,
                            val value: String) {

    def this() {
        this(null, null, null)
    }

    def this(name: String, value: String) {
        this(null, name, value)
    }

    override def toString: String = new ToStringBuilder(this)
            .append(id)
            .append(name)
            .append(value)
            .toString

    override def equals(obj: Any): Boolean =  obj match {
        case other: GameVariationProperty => other.getClass == getClass &&
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
