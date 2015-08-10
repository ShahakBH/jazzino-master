package strata.server.lobby.controlcentre.model

import org.joda.time.DateTime
import com.yazino.platform.tournament.{RecurringTournament, DayPeriod}
import org.apache.commons.lang3.builder.{EqualsBuilder, ToStringBuilder}
import org.apache.commons.lang.builder.HashCodeBuilder
import java.math.BigInteger
import strata.server.lobby.controlcentre.repository.TournamentVariationRepository
import scala.collection.JavaConversions._

class RecurringTournamentDefinition(val id: BigDecimal,
                                    val tournamentName: String,
                                    val tournamentDescription: String,
                                    val partnerId: String,
                                    val initialSignupTime: DateTime,
                                    val signupPeriod: Long,
                                    val frequency: Long,
                                    val variationId: BigDecimal,
                                    val enabled: Boolean,
                                    val exclusionPeriods: Seq[DayPeriod]) {

    def toPlatform(tournamentVariationRepository: TournamentVariationRepository): RecurringTournament =
        new RecurringTournament(
            toJava(id),
            initialSignupTime,
            signupPeriod,
            frequency,
            exclusionPeriods,
            tournamentVariationRepository.findById(variationId).map(_.toPlatform).getOrElse(null),
            tournamentName,
            tournamentDescription,
            partnerId,
            enabled)

    def withId(newId: BigDecimal): RecurringTournamentDefinition = new RecurringTournamentDefinition(newId, tournamentName, tournamentDescription,
        partnerId, initialSignupTime, signupPeriod, frequency, variationId, enabled, exclusionPeriods)

    private def toJava(value: BigDecimal) = if (value != null) BigInteger.valueOf(id.longValue()) else null

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
        case other: RecurringTournamentDefinition => other.getClass == getClass &&
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
