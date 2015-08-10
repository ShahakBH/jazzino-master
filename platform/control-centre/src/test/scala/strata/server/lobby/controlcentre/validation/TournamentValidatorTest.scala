package strata.server.lobby.controlcentre.validation

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.springframework.validation.Errors
import org.junit.{Test, After, Before}
import org.joda.time.DateTime
import strata.server.lobby.controlcentre.form.TournamentForm
import com.yazino.platform.tournament.TournamentStatus
import org.mockito.Mockito._
import com.yazino.test.ThreadLocalDateTimeUtils

class TournamentValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]

    private val underTest = new TournamentValidator

    @Before def lockSystemTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 10, 21, 0, 0, 0, 0).getMillis)
    }

    @After def unlockSystemTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem()
    }

    @Test def tournamentFormShouldBeSupported() {
        underTest.supports(classOf[TournamentForm]) should equal(true)
    }

    @Test def aTournamentWithNoNameShouldFail() {
        val tournament = aTournament
        tournament.setName(null)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("name", "tournament.name.missing", "Name is required")
    }

    @Test def aTournamentWithANameLongerThan255CharactersShouldFail() {
        val tournament = aTournament
        tournament.setName("a" * 256)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("name", "tournament.name.invalidLength",
            "Name cannot be longer than 255 characters")
    }

    @Test def aTournamentWithNoVariationIdShouldFail() {
        val tournament = aTournament
        tournament.setVariationId(null)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("variationId", "tournament.variationId.missing", "Variation is required")
    }

    @Test def aTournamentWithNoSignupStartShouldFail() {
        val tournament = aTournament
        tournament.setSignupStart(null)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("signupStart", "tournament.signupStart.missing", "Sign-up Start is required")
    }

    @Test def aTournamentWithASignupStartAfterTheSignupEndShouldFail() {
        val tournament = aTournament
        tournament.setSignupStart(new DateTime(2012, 10, 20, 1, 0, 0, 0).toDate)
        tournament.setSignupEnd(new DateTime(2012, 10, 20, 0, 0, 0, 0).toDate)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("signupStart", "tournament.signupStart.inconsistent",
            "Sign-up Start must be before the Sign-up End date")
    }

    @Test def aTournamentWithNoSignupEndShouldFail() {
        val tournament = aTournament
        tournament.setSignupEnd(null)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("signupEnd", "tournament.signupEnd.missing", "Sign-up End is required")
    }

    @Test def aTournamentWithASignupEndAfterTheStartShouldFail() {
        val tournament = aTournament
        tournament.setSignupEnd(new DateTime(2012, 10, 20, 1, 0, 0, 0).toDate)
        tournament.setStart(new DateTime(2012, 10, 20, 0, 0, 0, 0).toDate)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("signupEnd", "tournament.signupEnd.inconsistent",
            "Sign-up End must be before the Start date")
    }

    @Test def aTournamentWithASignupEndInThePastShouldFail() {
        val tournament = aTournament
        tournament.setSignupEnd(new DateTime(2012, 10, 20, 0, 0, 0, 0).toDate)
        tournament.setStart(new DateTime(2012, 10, 20, 0, 0, 0, 0).toDate)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("signupEnd", "tournament.signupEnd.past",
            "Sign-up End must be in the future")
    }

    @Test def aTournamentWithNoStartShouldFail() {
        val tournament = aTournament
        tournament.setStart(null)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("start", "tournament.start.missing", "Start is required")
    }

    @Test def aTournamentWithAStartInThePastShouldFail() {
        val tournament = aTournament
        tournament.setStart(new DateTime(2012, 10, 20, 0, 0, 0, 0).toDate)

        underTest.validate(tournament, errors)

        verify(errors).rejectValue("start", "tournament.start.past",
            "Start must be in the future")
    }

    @Test def aValidTournamentShouldPass() {
        underTest.validate(aTournament, errors)

        verifyZeroInteractions(errors)
    }

    private def aTournament = new TournamentForm(
        BigDecimal(11),
        "aName",
        BigDecimal(100),
        new DateTime(2012, 10, 20, 0, 0, 0, 0).toDate,
        new DateTime(2012, 10, 22, 0, 0, 0, 0).toDate,
        new DateTime(2012, 10, 22, 0, 0, 0, 0).toDate,
        TournamentStatus.RUNNING,
        "PLAY_FOR_FUN",
        "aDescription")
}
