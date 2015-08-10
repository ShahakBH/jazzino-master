package strata.server.lobby.controlcentre.validation

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.AssertionsForJUnit
import com.yazino.platform.tournament.TournamentType
import strata.server.lobby.controlcentre.model.Allocator
import org.scalatest.mock.MockitoSugar
import org.springframework.validation.Errors
import org.mockito.Mockito._
import org.junit.Test
import strata.server.lobby.controlcentre.form.{TournamentVariationPayoutForm, TournamentVariationRoundForm, TournamentVariationForm}
import java.util

class TournamentVariationValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]
    private val roundValidator = mock[TournamentVariationRoundValidator]
    private val payoutValidator = mock[TournamentVariationPayoutValidator]

    private val underTest = new TournamentVariationValidator(payoutValidator, roundValidator)

    @Test def theValidatorAcceptsTournamentVariationForms() {
        underTest.supports(classOf[TournamentVariationForm]) should equal(true)
    }

    @Test def aValidVariationPasses() {
        underTest.validate(aVariation(), errors)

        verifyZeroInteractions(errors)
    }

    @Test def aMissingNameFails() {
        val variation = aVariation()
        variation.setName(null)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("name", "variation.name.missing", "Name is required")
    }

    @Test def aNameThatIsTooLongFails() {
        val variation = aVariation()
        variation.setName("a" * 256)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("name", "variation.name.invalidLength", "Name cannot be longer than 255 characters")
    }

    @Test def aMissingTournamentTypeFails() {
        val variation = aVariation()
        variation.setTournamentType(null)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("tournamentType", "variation.tournamentType.missing", "Tournament Type is required")
    }

    @Test def aMissingEntryFeeFails() {
        val variation = aVariation()
        variation.setEntryFee(null)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("entryFee", "variation.entryFee.missing", "Entry Fee is required")
    }

    @Test def anInvalidEntryFeeFails() {
        val variation = aVariation()
        variation.setEntryFee(BigDecimal(-1))

        underTest.validate(variation, errors)

        verify(errors).rejectValue("entryFee", "variation.entryFee.invalid", "Entry Fee must be zero or above")
    }

    @Test def aMissingServiceFeeFails() {
        val variation = aVariation()
        variation.setServiceFee(null)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("serviceFee", "variation.serviceFee.missing", "Service Fee is required")
    }

    @Test def anInvalidServiceFeeFails() {
        val variation = aVariation()
        variation.setServiceFee(BigDecimal(-1))

        underTest.validate(variation, errors)

        verify(errors).rejectValue("serviceFee", "variation.serviceFee.invalid", "Service Fee must be zero or above")
    }

    @Test def anInvalidPrizePoolFails() {
        val variation = aVariation()
        variation.setPrizePool(BigDecimal(-1))

        underTest.validate(variation, errors)

        verify(errors).rejectValue("prizePool", "variation.prizePool.invalid",
            "Prize Pool must be zero or above if specified")
    }

    @Test def aMissingStartingChipsFails() {
        val variation = aVariation()
        variation.setStartingChips(null)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("startingChips", "variation.startingChips.missing", "Starting Chips is required")
    }

    @Test def anInvalidStartingChipsFails() {
        val variation = aVariation()
        variation.setStartingChips(BigDecimal(-1))

        underTest.validate(variation, errors)

        verify(errors).rejectValue("startingChips", "variation.startingChips.invalid", "Starting Chips must be zero or above")
    }

    @Test def anInvalidMinPlayersFails() {
        val variation = aVariation()
        variation.setMinPlayers(-1)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("minPlayers", "variation.minPlayers.invalid", "Minimum Players must be 0 or above")
    }

    @Test def anInvalidMaxPlayersFails() {
        val variation = aVariation()
        variation.setMaxPlayers(1)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("maxPlayers", "variation.maxPlayers.invalid", "Maximum Players must be 2 or above")
    }

    @Test def aMismatchedMinAndMaxPlayersFails() {
        val variation = aVariation()
        variation.setMinPlayers(4)
        variation.setMaxPlayers(3)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("maxPlayers", "variation.maxPlayers.mismatch",
            "Maximum Players must be greater than Minimum Players")
    }

    @Test def aMissingGameTypeFails() {
        val variation = aVariation()
        variation.setGameType(null)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("gameType", "variation.gameType.missing", "Game Type is required")
    }

    @Test def anInvalidExpiryDelayFails() {
        val variation = aVariation()
        variation.setExpiryDelay(-1)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("expiryDelay", "variation.expiryDelay.invalid", "Expiry Delay must be 0 or above")
    }

    @Test def aMissingAllocatorFails() {
        val variation = aVariation()
        variation.setAllocator(null)

        underTest.validate(variation, errors)

        verify(errors).rejectValue("allocator", "variation.allocator.missing", "Allocator is required")
    }

    @Test def allRoundsAreValidated() {
        val variation = aVariation()
        variation.setRounds(List(aRound(1), aRound(2)))

        underTest.validate(variation, errors)

        verify(errors).pushNestedPath("rounds[0]")
        verify(errors).pushNestedPath("rounds[1]")
        verify(errors, times(2)).popNestedPath()
        verify(roundValidator).validate(aRound(1), errors)
        verify(roundValidator).validate(aRound(2), errors)
    }

    @Test def roundsWithDuplicateNumbersFail() {
        val variation = aVariation()
        variation.setRounds(List(aRound(1), aRound(1)))

        underTest.validate(variation, errors)

        verify(errors).rejectValue("number", "variation-round.number.duplicate", "Round Numbers must be unique")
    }

    @Test def allPayoutsAreValidated() {
        val variation = aVariation()
        variation.setPayouts(List(aPayout(1), aPayout(2)))

        underTest.validate(variation, errors)

        verify(errors).pushNestedPath("payouts[0]")
        verify(errors).pushNestedPath("payouts[1]")
        verify(errors, times(2)).popNestedPath()
        verify(payoutValidator).validate(aPayout(1), errors)
        verify(payoutValidator).validate(aPayout(2), errors)
    }

    @Test def payoutsWithDuplicationRanksFail() {
        val variation = aVariation()
        variation.setPayouts(List(aPayout(1), aPayout(1)))

        underTest.validate(variation, errors)

        verify(errors).rejectValue("rank", "variation-payout.rank.duplicate", "Ranks must be unique")
    }

    @Test def payoutsWithPayoutsThatAreGreaterThanOneFail() {
        val variation = aVariation()
        variation.setPayouts(List(aPayout(1), aPayout(2), aPayout(3)))

        underTest.validate(variation, errors)

        verify(errors).rejectValue("payout", "variation-payout.payout.invalid", "All payouts must add to 1.0")
    }

    @Test def payoutsWithPayoutsThatAreLessThanOneFail() {
        val variation = aVariation()
        variation.setPayouts(List(aPayout(1)))

        underTest.validate(variation, errors)

        verify(errors).rejectValue("payout", "variation-payout.payout.invalid", "All payouts must add to 1.0")
    }

    private def aVariation() =
        new TournamentVariationForm(null, TournamentType.PRESET, "aTestTournament", BigDecimal(100),
            BigDecimal(200), BigDecimal(1000), 3, 10, "BLACKJACK", 60 * 60 * 24, BigDecimal(1500),
            Allocator.EVEN_BY_BALANCE, new util.ArrayList(), new util.ArrayList())

    private def aRound(round: Int) = new TournamentVariationRoundForm(BigDecimal(round), round, 30, 300, 1,
        "Red Blackjack", BigDecimal(0), "aTestRound")

    private def aPayout(rank: Int) = new TournamentVariationPayoutForm(BigDecimal(rank), rank, BigDecimal("0.5"))

}
