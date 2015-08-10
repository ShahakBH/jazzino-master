package strata.server.lobby.controlcentre.validation

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.springframework.validation.Errors
import org.junit.Test
import strata.server.lobby.controlcentre.form.{TrophyLeaderboardPositionForm, TimePeriod, TrophyLeaderboardForm}
import org.joda.time.DateTime
import org.mockito.Mockito._
import java.util

class TrophyLeaderboardValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]

    private val underTest = new TrophyLeaderboardValidator

    @Test def tournamentFormShouldBeSupported() {
        underTest.supports(classOf[TrophyLeaderboardForm]) should equal(true)
    }

    @Test def aValidTrophyLeaderboardShouldPass() {
        underTest.validate(aTrophyLeaderboard, errors)

        verifyZeroInteractions(errors)
    }

    @Test def aTrophyLeaderboardWithNoNameShouldFail() {
        val leaderboard = aTrophyLeaderboard
        leaderboard.setName(null)

        underTest.validate(leaderboard, errors)

        verify(errors).rejectValue("name", "trophy-leaderboard.name.missing", "Name is required")
    }

    @Test def aTrophyLeaderboardWithNoGameTypeShouldFail() {
        val leaderboard = aTrophyLeaderboard
        leaderboard.setGameType(null)

        underTest.validate(leaderboard, errors)

        verify(errors).rejectValue("gameType", "trophy-leaderboard.gameType.missing", "Game Type is required")
    }

    @Test def aTrophyLeaderboardWithNoStartDateShouldFail() {
        val leaderboard = aTrophyLeaderboard
        leaderboard.setStartDate(null)

        underTest.validate(leaderboard, errors)

        verify(errors).rejectValue("startDate", "trophy-leaderboard.startDate.missing", "Start Date is required")
    }

    @Test def aTrophyLeaderboardWithAStartDateAfterTheEndDateShouldFail() {
        val leaderboard = aTrophyLeaderboard
        leaderboard.setStartDate(new DateTime(2012, 8, 2, 0, 0, 0, 0).toDate)

        underTest.validate(leaderboard, errors)

        verify(errors).rejectValue("startDate", "trophy-leaderboard.startDate.inconsistent",
            "Start Date must be before the End Date")
    }

    @Test def aTrophyLeaderboardWithNoEndDateShouldFail() {
        val leaderboard = aTrophyLeaderboard
        leaderboard.setEndDate(null)

        underTest.validate(leaderboard, errors)

        verify(errors).rejectValue("endDate", "trophy-leaderboard.endDate.missing", "End Date is required")
    }

    @Test def aTrophyLeaderboardWithNoCycleShouldFail() {
        val leaderboard = aTrophyLeaderboard
        leaderboard.setCycle(null)

        underTest.validate(leaderboard, errors)

        verify(errors).rejectValue("cycle", "trophy-leaderboard.cycle.missing", "Cycle is required")
    }

    private def aTrophyLeaderboard = new TrophyLeaderboardForm(null, "aName", "aGameType", true, 0,
        new DateTime(2012, 1, 1, 0, 0, 0, 0).toDate, new DateTime(2012, 8, 1, 0, 0, 0, 0).toDate,
        TimePeriod(600), new util.ArrayList[TrophyLeaderboardPositionForm]())
}
