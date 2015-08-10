package strata.server.lobby.controlcentre.form

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import com.yazino.platform.tournament.TournamentType
import scala.beans.BeanProperty
import strata.server.lobby.controlcentre.model.{TournamentVariation, Allocator}
import java.util
import scala.collection.JavaConversions._

class TournamentVariationForm(@BeanProperty var id: BigDecimal,
                              @BeanProperty var tournamentType: TournamentType,
                              @BeanProperty var name: String,
                              @BeanProperty var entryFee: BigDecimal,
                              @BeanProperty var serviceFee: BigDecimal,
                              @BeanProperty var startingChips: BigDecimal,
                              @BeanProperty var minPlayers: Int,
                              @BeanProperty var maxPlayers: Int,
                              @BeanProperty var gameType: String,
                              @BeanProperty var expiryDelay: Long,
                              @BeanProperty var prizePool: BigDecimal,
                              @BeanProperty var allocator: Allocator,
                              var rounds: util.List[TournamentVariationRoundForm],
                              var payouts: util.List[TournamentVariationPayoutForm]) {

    def this() {
        this(null, TournamentType.PRESET, null, BigDecimal(0), BigDecimal(0), BigDecimal(0),
            3, 50, null, 60 * 60 * 24, null, Allocator.EVEN_BY_BALANCE, new util.ArrayList(), new util.ArrayList())
    }

    def this(variation: TournamentVariation) {
        this(variation.id, variation.tournamentType, variation.name, variation.entryFee,
            variation.serviceFee, variation.startingChips, variation.minPlayers, variation.maxPlayers,
            variation.gameType, variation.expiryDelay, variation.prizePool, variation.allocator,
            new util.ArrayList(variation.rounds.map(new TournamentVariationRoundForm(_))),
            new util.ArrayList(variation.payouts.map(new TournamentVariationPayoutForm(_))))
    }

    def toVariation: TournamentVariation = new TournamentVariation(id, tournamentType, name, entryFee, serviceFee,
        startingChips, minPlayers, maxPlayers, gameType, expiryDelay, prizePool, allocator,
        rounds.map(_.toRound).toList, payouts.map(_.toPayout).toList)

    def getRounds: util.List[TournamentVariationRoundForm] = rounds

    def setRounds(newRounds: List[TournamentVariationRoundForm]) {
        if (newRounds != null) {
            rounds = new util.ArrayList(newRounds)
        } else {
            rounds = new util.ArrayList()
        }
    }

    def getPayouts: util.List[TournamentVariationPayoutForm] = payouts

    def setPayouts(newPayouts: List[TournamentVariationPayoutForm]) {
        if (newPayouts != null) {
            payouts = new util.ArrayList(newPayouts)
        } else {
            payouts = new util.ArrayList()
        }
    }

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
        case other: TournamentVariationForm => other.getClass == getClass &&
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
