package strata.server.lobby.controlcentre.form

import scala.beans.BeanProperty
import java.util
import org.joda.time.DateTime
import strata.server.lobby.controlcentre.model.SystemMessage
import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}

class SystemMessageForm(@BeanProperty var id: BigDecimal,
                        @BeanProperty var message: String,
                        @BeanProperty var validFrom: util.Date,
                        @BeanProperty var validTo: util.Date) {

    def this() {
        this(null, null,
            new DateTime().plusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).toDate,
            new DateTime().plusHours(2).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).toDate)
    }

    def this(systemMessage: SystemMessage) {
            this(systemMessage.id, systemMessage.message, systemMessage.validFrom.toDate, systemMessage.validTo.toDate)
    }

    def toSystemMessage: SystemMessage =
            new SystemMessage(id, message, new DateTime(validFrom.getTime), new DateTime(validTo.getTime))

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(message)
        .append(validFrom)
        .append(validTo)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: SystemMessageForm => other.getClass == getClass &&
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
