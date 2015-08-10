package strata.server.lobby.controlcentre.form

import org.joda.time.DateTime
import org.apache.commons.lang3.builder.{EqualsBuilder, ToStringBuilder}
import org.apache.commons.lang.builder.HashCodeBuilder
import java.util
import RecurringTournamentDefinitionForm._
import strata.server.lobby.controlcentre.model.RecurringTournamentDefinition
import scala.collection.JavaConversions._
import scala.beans.BeanProperty

object RecurringTournamentDefinitionForm {
    private val ONE_DAY = 86400000
    private val ONE_HOUR = 3600000
}

class RecurringTournamentDefinitionForm(@BeanProperty var id: BigDecimal,
                                        @BeanProperty var tournamentName: String,
                                        @BeanProperty var tournamentDescription: String,
                                        @BeanProperty var partnerId: String,
                                        @BeanProperty var initialSignupTime: util.Date,
                                        @BeanProperty var signupPeriod: TimePeriod,
                                        @BeanProperty var frequency: TimePeriod,
                                        @BeanProperty var variationId: BigDecimal,
                                        @BeanProperty var enabled: Boolean,
                                        @BeanProperty var exclusionPeriods: util.List[ExclusionPeriodForm]) {

    def this() {
        this(null, null, null, "PLAY_FOR_FUN", new DateTime().withTime(0, 0, 0, 0).toDate,
            TimePeriod(ONE_DAY), TimePeriod(ONE_HOUR), null, true, new util.ArrayList())
    }

    def this(definition: RecurringTournamentDefinition) {
        this(definition.id, definition.tournamentName,
            definition.tournamentDescription, definition.partnerId, definition.initialSignupTime.toDate,
            TimePeriod(definition.signupPeriod), TimePeriod(definition.frequency), definition.variationId,
            definition.enabled, definition.exclusionPeriods.map(new ExclusionPeriodForm(_)))
    }

    def toDefinition: RecurringTournamentDefinition = new RecurringTournamentDefinition(id, tournamentName, tournamentDescription,
        partnerId, toDateTime(initialSignupTime), signupPeriod.milliseconds, frequency.milliseconds,
        variationId, enabled, exclusionPeriods.map(_.toDayPeriod))

    private def toDateTime(date: util.Date) = if (date != null) new DateTime(date.getTime) else null

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(tournamentName)
        .append(tournamentDescription)
        .append(partnerId)
        .append(initialSignupTime)
        .append(signupPeriod)
        .append(frequency)
        .append(variationId)
        .append(enabled)
        .append(exclusionPeriods)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: RecurringTournamentDefinitionForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(id, other.id)
                .append(tournamentName, other.tournamentName)
                .append(tournamentDescription, other.tournamentDescription)
                .append(partnerId, other.partnerId)
                .append(initialSignupTime, other.initialSignupTime)
                .append(signupPeriod, other.signupPeriod)
                .append(frequency, other.frequency)
                .append(variationId, other.variationId)
                .append(enabled, other.enabled)
                .append(exclusionPeriods, other.exclusionPeriods)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(id)
        .append(tournamentName)
        .append(tournamentDescription)
        .append(partnerId)
        .append(initialSignupTime)
        .append(signupPeriod)
        .append(frequency)
        .append(variationId)
        .append(enabled)
        .append(exclusionPeriods)
        .hashCode

}
