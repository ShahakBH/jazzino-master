package strata.server.lobby.controlcentre.form

import scala.beans.BeanProperty
import org.apache.commons.lang3.builder.{ToStringBuilder, EqualsBuilder, HashCodeBuilder}
import TimePeriod._

object TimePeriod {
    private val ONE_DAY = 86400000L
    private val ONE_HOUR = 3600000L
    private val ONE_MINUTE = 60000L

    def apply(milliseconds: Long): TimePeriod = new TimePeriod(milliseconds)
}

class TimePeriod(@BeanProperty var days: Long,
                 @BeanProperty var hours: Long,
                 @BeanProperty var minutes: Long) {

    def this() {
        this(0, 0, 0)
    }

    def this(milliseconds: Long) {
        this()
        days = milliseconds / ONE_DAY
        hours = (milliseconds - (days * ONE_DAY)) / ONE_HOUR
        minutes = (milliseconds - (days * ONE_DAY) - (hours * ONE_HOUR)) / ONE_MINUTE
    }

    def milliseconds: Long = (days * ONE_DAY) + (hours * ONE_HOUR) + (minutes * ONE_MINUTE)

    override def toString: String = "%d days, %d hours, %d minutes".format(days, hours, minutes)

    override def equals(obj: Any): Boolean = obj match {
        case other: TimePeriod => other.getClass == getClass &&
            new EqualsBuilder()
                .append(days, other.days)
                .append(hours, other.hours)
                .append(minutes, other.minutes)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(days)
        .append(hours)
        .append(minutes)
        .hashCode

}
