package strata.server.lobby.controlcentre.form

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import scala.beans.BeanProperty
import strata.server.lobby.controlcentre.model.TournamentVariationRound

class TournamentVariationRoundForm(@BeanProperty var id: BigDecimal,
                                   @BeanProperty var number: Int,
                                   @BeanProperty var endInterval: Int,
                                   @BeanProperty var length: Int,
                                   @BeanProperty var gameVariationId: BigDecimal,
                                   @BeanProperty var clientPropertiesId: String,
                                   @BeanProperty var minimumBalance: BigDecimal,
                                   @BeanProperty var description: String) {

    def this() {
        this(null, 1, 0, 300000, null, null, BigDecimal(0), null)
    }

    def this(round: TournamentVariationRound) {
        this(round.id, round.number, round.endInterval, round.length, round.gameVariationId,
            round.clientPropertiesId, round.minimumBalance, round.description)
    }

    def toRound: TournamentVariationRound = new TournamentVariationRound(id, number, endInterval, length, gameVariationId,
        clientPropertiesId, minimumBalance, description)

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(number)
        .append(endInterval)
        .append(length)
        .append(gameVariationId)
        .append(clientPropertiesId)
        .append(minimumBalance)
        .append(description)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: TournamentVariationRoundForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(id, other.id)
                .append(number, other.number)
                .append(endInterval, other.endInterval)
                .append(length, other.length)
                .append(gameVariationId, other.gameVariationId)
                .append(clientPropertiesId, other.clientPropertiesId)
                .append(minimumBalance, other.minimumBalance)
                .append(description, other.description)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(id)
        .append(number)
        .append(endInterval)
        .append(length)
        .append(gameVariationId)
        .append(clientPropertiesId)
        .append(minimumBalance)
        .append(description)
        .hashCode

}
