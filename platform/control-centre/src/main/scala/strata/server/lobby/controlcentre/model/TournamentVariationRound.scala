package strata.server.lobby.controlcentre.model

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import com.yazino.platform.tournament.{TournamentVariationRound => PlatformRound}
import java.math

class TournamentVariationRound(val id: BigDecimal,
                               val number: Int,
                               val endInterval: Int,
                               val length: Int,
                               val gameVariationId: BigDecimal,
                               val clientPropertiesId: String,
                               val minimumBalance: BigDecimal,
                               val description: String) {

    def withId(newId: BigDecimal): TournamentVariationRound =
        new TournamentVariationRound(newId,
            number,
            endInterval,
            length,
            gameVariationId,
            clientPropertiesId,
            minimumBalance,
            description)

    def toPlatform: PlatformRound = new PlatformRound(number, endInterval, length, toJava(gameVariationId),
        clientPropertiesId, toJava(minimumBalance), description)

    private def toJava(number: BigDecimal): math.BigDecimal = if (number != null) number.underlying() else null

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
        case other: TournamentVariationRound => other.getClass == getClass &&
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
