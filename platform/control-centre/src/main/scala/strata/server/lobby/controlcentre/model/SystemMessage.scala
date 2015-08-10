package strata.server.lobby.controlcentre.model

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import org.joda.time.DateTime
import org.apache.commons.lang3.Validate.notNull

class SystemMessage(val id: BigDecimal,
                    val message: String,
                    val validFrom: DateTime,
                    val validTo: DateTime) {
    notNull(message, "message may not be null")
    notNull(validFrom, "validFrom may not be null")
    notNull(validTo, "validTo may not be null")

    def withId(newId: BigDecimal): SystemMessage = new SystemMessage(newId, message, validFrom, validTo)

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(message)
        .append(validFrom)
        .append(validTo)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: SystemMessage => other.getClass == getClass &&
            new EqualsBuilder()
                .append(id, other.id)
                .append(message, other.message)
                .append(validFrom, other.validFrom)
                .append(validTo, other.validTo)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(id)
        .append(message)
        .append(validFrom)
        .append(validTo)
        .hashCode
}
