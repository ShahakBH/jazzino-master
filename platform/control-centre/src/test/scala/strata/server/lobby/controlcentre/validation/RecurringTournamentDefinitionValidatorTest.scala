package strata.server.lobby.controlcentre.validation

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.springframework.validation.Errors
import org.junit.Test
import strata.server.lobby.controlcentre.form.{TimePeriod, RecurringTournamentDefinitionForm}
import org.mockito.Mockito._
import java.util

class RecurringTournamentDefinitionValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]

    private val underTest = new RecurringTournamentDefinitionValidator

    @Test def theValidatorAcceptsRecurringTournamentDefinitionForms() {
        underTest.supports(classOf[RecurringTournamentDefinitionForm]) should equal(true)
    }

    @Test def aValidDefinitionPasses() {
        underTest.validate(aDefinition, errors)

        verifyZeroInteractions(errors)
    }

    @Test def aMissingTournamentNameFails() {
        val payout = aDefinition
        payout.setTournamentName(null)

        underTest.validate(payout, errors)

        verify(errors).rejectValue("tournamentName", "definition.tournamentName.missing",
            "Tournament Name is required")
    }

    @Test def aMissingPartnerIDFails() {
        val payout = aDefinition
        payout.setPartnerId(null)

        underTest.validate(payout, errors)

        verify(errors).rejectValue("partnerId", "definition.partnerId.missing", "Partner ID is required")
    }

    @Test def aMissingVariationIDFails() {
        val payout = aDefinition
        payout.setVariationId(null)

        underTest.validate(payout, errors)

        verify(errors).rejectValue("variationId", "definition.variationId.missing", "Variation ID is required")
    }

    @Test def aMissingInitialSignupTimeFails() {
        val payout = aDefinition
        payout.setInitialSignupTime(null)

        underTest.validate(payout, errors)

        verify(errors).rejectValue("initialSignupTime", "definition.initialSignupTime.missing",
            "Initial Sign-up Time is required")
    }

    private def aDefinition = new RecurringTournamentDefinitionForm(BigDecimal(100),
        "aTournamentName", "aDescription", "aPartner", new util.Date(), TimePeriod(300),
        TimePeriod(500), BigDecimal(200), true, new util.ArrayList())

}
