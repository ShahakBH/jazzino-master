package strata.server.lobby.controlcentre.model

import com.yazino.platform.tournament.{TournamentDefinition, TournamentStatus}
import org.joda.time.DateTime
import java.math
import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import strata.server.lobby.controlcentre.repository.TournamentVariationRepository

class Tournament(val id: BigDecimal,
                 val name: String,
                 val variationId: BigDecimal,
                 val signupStart: DateTime,
                 val signupEnd: DateTime,
                 val start: DateTime,
                 val status: TournamentStatus,
                 val partnerId: String,
                 val description: String) {

    def toPlatform(tournamentVariationRepository: TournamentVariationRepository): TournamentDefinition =
        new TournamentDefinition(toJava(id), name,
            tournamentVariationRepository.findById(variationId).map(_.toPlatform).getOrElse(null),
            signupStart, signupEnd, start, status, partnerId, description)

    private def toJava(number: BigDecimal): math.BigDecimal = if (number != null) number.underlying() else null

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(name)
        .append(variationId)
        .append(signupStart)
        .append(signupEnd)
        .append(start)
        .append(status)
        .append(partnerId)
        .append(description)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: Tournament => other.getClass == getClass &&
            new EqualsBuilder()
                .append(id, other.id)
                .append(name, other.name)
                .append(variationId, other.variationId)
                .append(signupStart, other.signupStart)
                .append(signupEnd, other.signupEnd)
                .append(start, other.start)
                .append(status, other.status)
                .append(partnerId, other.partnerId)
                .append(description, other.description)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(id)
        .append(name)
        .append(variationId)
        .append(signupStart)
        .append(signupEnd)
        .append(start)
        .append(status)
        .append(partnerId)
        .append(description)
        .hashCode

}
