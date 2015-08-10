package strata.server.lobby.controlcentre.form

import scala.beans.BeanProperty
import com.yazino.platform.tournament.DayPeriod
import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}

class ExclusionPeriodForm(@BeanProperty var day: Int,
                          @BeanProperty var fromHour: Int,
                          @BeanProperty var fromMinute: Int,
                          @BeanProperty var toHour: Int,
                          @BeanProperty var toMinute: Int) {

    def this() {
        this(1, 0, 0, 23, 59)
    }

    def this(dayPeriod: DayPeriod) {
        this(dayPeriod.getDay,
            dayPeriod.getStartTime.getHourOfDay, dayPeriod.getStartTime.getMinuteOfHour,
            dayPeriod.getEndTime.getHourOfDay, dayPeriod.getEndTime.getMinuteOfHour)
    }

    def toDayPeriod: DayPeriod = {
        val dayPeriod = new DayPeriod()
        dayPeriod.setDay(day)
        dayPeriod.setStartTime("%02d:%02d".format(fromHour, fromMinute))
        dayPeriod.setEndTime("%02d:%02d".format(toHour, toMinute))
        dayPeriod
    }

    override def toString: String = new ToStringBuilder(this)
        .append(day)
        .append(fromHour)
        .append(fromMinute)
        .append(toHour)
        .append(toMinute)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: ExclusionPeriodForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(day, other.day)
                .append(fromHour, other.fromHour)
                .append(fromMinute, other.fromMinute)
                .append(toHour, other.toHour)
                .append(toMinute, other.toMinute)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(day)
        .append(fromHour)
        .append(fromMinute)
        .append(toHour)
        .append(toMinute)
        .hashCode

}

