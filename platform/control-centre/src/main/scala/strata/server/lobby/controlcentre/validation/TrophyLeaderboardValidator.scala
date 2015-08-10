package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.TrophyLeaderboardForm
import org.springframework.stereotype.Component

@Component
class TrophyLeaderboardValidator extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[TrophyLeaderboardForm]

    def validate(target: Any, errors: Errors) {
        val leaderboard = target.asInstanceOf[TrophyLeaderboardForm]

        if (leaderboard.name == null) {
            errors.rejectValue("name", "trophy-leaderboard.name.missing", "Name is required")
        }

        if (leaderboard.gameType == null) {
            errors.rejectValue("gameType", "trophy-leaderboard.gameType.missing", "Game Type is required")
        }

        if (leaderboard.startDate == null) {
            errors.rejectValue("startDate", "trophy-leaderboard.startDate.missing", "Start Date is required")

        } else if (leaderboard.endDate != null
            && leaderboard.startDate.after(leaderboard.endDate)) {
            errors.rejectValue("startDate", "trophy-leaderboard.startDate.inconsistent",
                "Start Date must be before the End Date")
        }

        if (leaderboard.endDate == null) {
            errors.rejectValue("endDate", "trophy-leaderboard.endDate.missing", "End Date is required")
        }

        if (leaderboard.cycle == null) {
            errors.rejectValue("cycle", "trophy-leaderboard.cycle.missing", "Cycle is required")
        }
    }
}
