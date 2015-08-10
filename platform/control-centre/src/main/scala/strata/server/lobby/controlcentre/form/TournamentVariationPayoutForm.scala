package strata.server.lobby.controlcentre.form

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import scala.beans.BeanProperty
import strata.server.lobby.controlcentre.model.TournamentVariationPayout

class TournamentVariationPayoutForm(@BeanProperty var id: BigDecimal,
                                    @BeanProperty var rank: Int,
                                    @BeanProperty var payout: BigDecimal) {

    def this() {
        this(null, 1, BigDecimal(0))
    }

    def this(payout: TournamentVariationPayout) {
        this(payout.id, payout.rank, payout.payout)
    }

    def toPayout: TournamentVariationPayout = new TournamentVariationPayout(id, rank, payout)

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(rank)
        .append(payout)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: TournamentVariationPayoutForm => other.getClass == getClass &&
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
