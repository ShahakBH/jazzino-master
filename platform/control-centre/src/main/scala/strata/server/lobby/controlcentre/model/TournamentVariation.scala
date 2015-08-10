package strata.server.lobby.controlcentre.model

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import com.yazino.platform.tournament.{TournamentVariationTemplate, TournamentType}
import scala.collection.JavaConversions._
import java.util
import java.math

class TournamentVariation(val id: BigDecimal,
                          val tournamentType: TournamentType,
                          val name: String,
                          val entryFee: BigDecimal,
                          val serviceFee: BigDecimal,
                          val startingChips: BigDecimal,
                          val minPlayers: Int,
                          val maxPlayers: Int,
                          val gameType: String,
                          val expiryDelay: Long,
                          val prizePool: BigDecimal,
                          val allocator: Allocator,
                          val rounds: List[TournamentVariationRound],
                          val payouts: List[TournamentVariationPayout]) {

    def withId(newId: BigDecimal): TournamentVariation =
        new TournamentVariation(newId,
            tournamentType,
            name,
            entryFee,
            serviceFee,
            startingChips,
            minPlayers,
            maxPlayers,
            gameType,
            expiryDelay,
            prizePool,
            allocator,
            rounds,
            payouts)

    def withRounds(newRounds: Iterable[TournamentVariationRound]): TournamentVariation =
        new TournamentVariation(id,
            tournamentType,
            name,
            entryFee,
            serviceFee,
            startingChips,
            minPlayers,
            maxPlayers,
            gameType,
            expiryDelay,
            prizePool,
            allocator,
            newRounds.toList,
            payouts)

    def withPayouts(newPayouts: Iterable[TournamentVariationPayout]): TournamentVariation =
        new TournamentVariation(id,
            tournamentType,
            name,
            entryFee,
            serviceFee,
            startingChips,
            minPlayers,
            maxPlayers,
            gameType,
            expiryDelay,
            prizePool,
            allocator,
            rounds,
            newPayouts.toList)

    def toPlatform: TournamentVariationTemplate =
        new TournamentVariationTemplate(toJava(id), tournamentType, name, toJava(entryFee), toJava(serviceFee),
            toJava(prizePool), toJava(startingChips), minPlayers, maxPlayers, gameType, expiryDelay, allocator.name(),
            new util.ArrayList(payouts.map(_.toPlatform)), new util.ArrayList(rounds.map(_.toPlatform)))

    private def toJava(number: BigDecimal): math.BigDecimal = if (number != null) number.underlying() else null

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(tournamentType)
        .append(name)
        .append(entryFee)
        .append(serviceFee)
        .append(startingChips)
        .append(minPlayers)
        .append(maxPlayers)
        .append(gameType)
        .append(expiryDelay)
        .append(prizePool)
        .append(allocator)
        .append(rounds)
        .append(payouts)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: TournamentVariation => other.getClass == getClass &&
            new EqualsBuilder()
                .append(id, other.id)
                .append(tournamentType, other.tournamentType)
                .append(name, other.name)
                .append(entryFee, other.entryFee)
                .append(serviceFee, other.serviceFee)
                .append(startingChips, other.startingChips)
                .append(minPlayers, other.minPlayers)
                .append(maxPlayers, other.maxPlayers)
                .append(gameType, other.gameType)
                .append(expiryDelay, other.expiryDelay)
                .append(prizePool, other.prizePool)
                .append(allocator, other.allocator)
                .append(rounds, other.rounds)
                .append(payouts, other.payouts)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(id)
        .append(tournamentType)
        .append(name)
        .append(entryFee)
        .append(serviceFee)
        .append(startingChips)
        .append(minPlayers)
        .append(maxPlayers)
        .append(gameType)
        .append(expiryDelay)
        .append(prizePool)
        .append(allocator)
        .append(rounds)
        .append(payouts)
        .hashCode

}
