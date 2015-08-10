package strata.server.lobby.controlcentre.validation

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.springframework.validation.Errors
import org.junit.Test
import strata.server.lobby.controlcentre.form.TournamentVariationRoundForm
import org.mockito.Mockito._

class TournamentVariationRoundValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]

    private val underTest = new TournamentVariationRoundValidator

    @Test def theValidatorAcceptsTournamentVariationRoundForms() {
        underTest.supports(classOf[TournamentVariationRoundForm]) should equal(true)
    }

    @Test def aValidRoundPasses() {
        underTest.validate(aRound(), errors)

        verifyZeroInteractions(errors)
    }

    @Test def anInvalidNumberFails() {
        val round = aRound()
        round.setNumber(0)

        underTest.validate(round, errors)

        verify(errors).rejectValue("number", "variation-round.number.invalid", "Number must be 1 or above")
    }

    @Test def anInvalidEndIntervalFails() {
        val round = aRound()
        round.setEndInterval(-1)

        underTest.validate(round, errors)

        verify(errors).rejectValue("endInterval", "variation-round.endInterval.invalid",
            "End Interval must be 0 or above")
    }

    @Test def anInvalidLengthFails() {
        val round = aRound()
        round.setLength(0)

        underTest.validate(round, errors)

        verify(errors).rejectValue("length", "variation-round.length.invalid", "Length must be 1 or above")
    }

    @Test def aMissingMinimumBalanceFails() {
        val round = aRound()
        round.setMinimumBalance(null)

        underTest.validate(round, errors)

        verify(errors).rejectValue("minimumBalance", "variation-round.minimumBalance.missing",
            "Minimum Balance is required")
    }


    @Test def anInvalidMinimumBalanceFails() {
        val round = aRound()
        round.setMinimumBalance(BigDecimal(-1))

        underTest.validate(round, errors)

        verify(errors).rejectValue("minimumBalance", "variation-round.minimumBalance.invalid",
            "Minimum Balance must be 0 or above")
    }

    @Test def aMissingDescriptionFails() {
        val round = aRound()
        round.setDescription(null)

        underTest.validate(round, errors)

        verify(errors).rejectValue("description", "variation-round.description.missing", "Description is required")
    }

    private def aRound() = new TournamentVariationRoundForm(BigDecimal(10), 1, 30, 300, 1, "Red Blackjack",
        BigDecimal(0), "aTestRound")
}
