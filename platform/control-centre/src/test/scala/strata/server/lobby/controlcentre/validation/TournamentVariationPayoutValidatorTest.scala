package strata.server.lobby.controlcentre.validation

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.springframework.validation.Errors
import org.junit.Test
import strata.server.lobby.controlcentre.form.TournamentVariationPayoutForm
import org.mockito.Mockito._

class TournamentVariationPayoutValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]

    private val underTest = new TournamentVariationPayoutValidator

    @Test def theValidatorAcceptsTournamentVariationPayoutForms() {
        underTest.supports(classOf[TournamentVariationPayoutForm]) should equal (true)
    }

    @Test def aValidPayoutPasses() {
        underTest.validate(aPayout(), errors)

        verifyZeroInteractions(errors)
    }

    @Test def anInvalidRankFails() {
        val payout = aPayout()
        payout.setRank(0)

        underTest.validate(payout, errors)

        verify(errors).rejectValue("rank", "variation-payout.rank.invalid", "Rank must be 1 or above")
    }

    @Test def aMissingPayoutFails() {
        val payout = aPayout()
        payout.setPayout(null)

        underTest.validate(payout, errors)

        verify(errors).rejectValue("payout", "variation-payout.payout.missing", "Payout is required")
    }

    @Test def aPayoutBelowZeroFails() {
        val payout = aPayout()
        payout.setPayout(BigDecimal("-0.1"))

        underTest.validate(payout, errors)

        verify(errors).rejectValue("payout", "variation-payout.payout.invalid", "Payout must be between 0 and 1")
    }

    @Test def aPayoutAboveOneFails() {
        val payout = aPayout()
        payout.setPayout(BigDecimal("1.1"))

        underTest.validate(payout, errors)

        verify(errors).rejectValue("payout", "variation-payout.payout.invalid", "Payout must be between 0 and 1")
    }

    private def aPayout() = new TournamentVariationPayoutForm(BigDecimal(10), 1, BigDecimal("0.5"))

}
