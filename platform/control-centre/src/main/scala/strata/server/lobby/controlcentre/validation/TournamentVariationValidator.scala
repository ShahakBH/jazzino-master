package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.TournamentVariationForm
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import scala.collection.JavaConversions._

@Component
class TournamentVariationValidator @Autowired()(val payoutValidator: TournamentVariationPayoutValidator,
                                                val roundValidator: TournamentVariationRoundValidator)
    extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[TournamentVariationForm]

    def validate(target: Any, errors: Errors) {
        val variation = target.asInstanceOf[TournamentVariationForm]

        if (variation.getName == null) {
            errors.rejectValue("name", "variation.name.missing",
                "Name is required")
        } else if (variation.getName.length < 1 || variation.getName.length > 255) {
            errors.rejectValue("name", "variation.name.invalidLength",
                "Name cannot be longer than 255 characters")
        }

        if (variation.getTournamentType == null) {
            errors.rejectValue("tournamentType", "variation.tournamentType.missing",
                "Tournament Type is required")
        }

        if (variation.getEntryFee == null) {
            errors.rejectValue("entryFee", "variation.entryFee.missing",
                "Entry Fee is required")
        } else if (variation.getEntryFee.compare(BigDecimal(0)) < 0) {
            errors.rejectValue("entryFee", "variation.entryFee.invalid",
                "Entry Fee must be zero or above")
        }

        if (variation.getServiceFee == null) {
            errors.rejectValue("serviceFee", "variation.serviceFee.missing",
                "Service Fee is required")
        } else if (variation.getServiceFee.compare(BigDecimal(0)) < 0) {
            errors.rejectValue("serviceFee", "variation.serviceFee.invalid",
                "Service Fee must be zero or above")
        }

        if (variation.getPrizePool != null
                && variation.getPrizePool.compare(BigDecimal(0)) < 0) {
            errors.rejectValue("prizePool", "variation.prizePool.invalid",
                "Prize Pool must be zero or above if specified")
        }

        if (variation.getStartingChips == null) {
            errors.rejectValue("startingChips", "variation.startingChips.missing",
                "Starting Chips is required")
        } else if (variation.getStartingChips.compare(BigDecimal(0)) < 0) {
            errors.rejectValue("startingChips", "variation.startingChips.invalid",
                "Starting Chips must be zero or above")
        }

        if (variation.getMinPlayers < 0) {
            errors.rejectValue("minPlayers", "variation.minPlayers.invalid",
                "Minimum Players must be 0 or above")
        }

        if (variation.getMaxPlayers < 2) {
            errors.rejectValue("maxPlayers", "variation.maxPlayers.invalid",
                "Maximum Players must be 2 or above")
        } else if (variation.getMinPlayers >= variation.getMaxPlayers) {
            errors.rejectValue("maxPlayers", "variation.maxPlayers.mismatch",
                "Maximum Players must be greater than Minimum Players")
        }

        if (variation.getGameType == null) {
            errors.rejectValue("gameType", "variation.gameType.missing",
                "Game Type is required")
        }

        if (variation.getExpiryDelay < 0) {
            errors.rejectValue("expiryDelay", "variation.expiryDelay.invalid",
                "Expiry Delay must be 0 or above")
        }

        if (variation.getAllocator == null) {
            errors.rejectValue("allocator", "variation.allocator.missing",
                "Allocator is required")
        }

        validateRounds(variation, errors)
        validatePayouts(variation, errors)
    }


    private def validateRounds(variation: TournamentVariationForm, errors: Errors) {
        var index = 0
        var roundNumbers = Set[Int]()
        variation.rounds.foreach {
            round =>
                errors.pushNestedPath("rounds[" + index + "]")
                roundValidator.validate(round, errors)

                if (roundNumbers.contains(round.getNumber)) {
                    errors.rejectValue("number", "variation-round.number.duplicate",
                        "Round Numbers must be unique")
                } else {
                    roundNumbers = roundNumbers + round.getNumber
                }

                errors.popNestedPath()
                index += 1
        }
    }

    private def validatePayouts(variation: TournamentVariationForm, errors: Errors) {
        var index = 0
        var ranks = Set[Int]()
        var totalPayout = BigDecimal(0)
        variation.payouts.foreach {
            payout =>
                errors.pushNestedPath("payouts[" + index + "]")
                payoutValidator.validate(payout, errors)

                if (ranks.contains(payout.getRank)) {
                    errors.rejectValue("rank", "variation-payout.rank.duplicate",
                        "Ranks must be unique")
                } else {
                    ranks = ranks + payout.getRank
                }

                totalPayout += payout.getPayout
                if (totalPayout.compare(BigDecimal(1)) > 0) {
                    errors.rejectValue("payout", "variation-payout.payout.invalid",
                        "All payouts must add to 1.0")
                }

                errors.popNestedPath()
                index += 1
        }

        if (!variation.getPayouts.isEmpty && totalPayout.compare(BigDecimal(1)) < 0) {
            errors.pushNestedPath("payouts[0]")
            errors.rejectValue("payout", "variation-payout.payout.invalid",
                "All payouts must add to 1.0")
            errors.popNestedPath()
        }
    }
}
