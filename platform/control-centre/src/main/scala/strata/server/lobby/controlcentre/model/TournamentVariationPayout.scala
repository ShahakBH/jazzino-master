package strata.server.lobby.controlcentre.model

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import com.yazino.platform.tournament.{TournamentVariationPayout => PlatformPayout}

class TournamentVariationPayout(val id: BigDecimal,
                                val rank: Int,
                                val payout: BigDecimal) {

    def withId(newId: BigDecimal): TournamentVariationPayout = new TournamentVariationPayout(newId, rank, payout)

    def toPlatform: PlatformPayout = new PlatformPayout(rank, if (payout != null) payout.underlying() else null)

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(rank)
        .append(payout)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: TournamentVariationPayout => other.getClass == getClass &&
            new EqualsBuilder()
                .append(id, other.id)
                .append(rank, other.rank)
                .append(payout, other.payout)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(id)
        .append(rank)
        .append(payout)
        .hashCode

}
