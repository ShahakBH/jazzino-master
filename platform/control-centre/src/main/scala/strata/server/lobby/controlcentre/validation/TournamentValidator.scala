package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.TournamentForm
import org.springframework.stereotype.Component
import org.joda.time.DateTime

@Component
class TournamentValidator extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[TournamentForm]

    def validate(target: Any, errors: Errors) {
        val tournament = target.asInstanceOf[TournamentForm]

        if (tournament.name == null) {
            errors.rejectValue("name", "tournament.name.missing", "Name is required")
        } else if (tournament.name.length < 1 || tournament.name.length > 255) {
            errors.rejectValue("name", "tournament.name.invalidLength",
                "Name cannot be longer than 255 characters")
        }

        if (tournament.variationId == null) {
            errors.rejectValue("variationId", "tournament.variationId.missing", "Variation is required")
        }

        if (tournament.signupStart == null) {
            errors.rejectValue("signupStart", "tournament.signupStart.missing", "Sign-up Start is required")

        } else if (tournament.signupEnd != null
            && tournament.signupStart.after(tournament.signupEnd)) {
            errors.rejectValue("signupStart", "tournament.signupStart.inconsistent",
                "Sign-up Start must be before the Sign-up End date")

        }

        if (tournament.signupEnd == null) {
            errors.rejectValue("signupEnd", "tournament.signupEnd.missing", "Sign-up End is required")

        } else if (tournament.start != null
            && tournament.signupEnd.after(tournament.start)) {
            errors.rejectValue("signupEnd", "tournament.signupEnd.inconsistent",
                "Sign-up End must be before the Start date")

        } else if (tournament.signupEnd.before(new DateTime().toDate)) {
            errors.rejectValue("signupEnd", "tournament.signupEnd.past", "Sign-up End must be in the future")
        }

        if (tournament.start == null) {
            errors.rejectValue("start", "tournament.start.missing", "Start is required")

        } else if (tournament.start.before(new DateTime().toDate)) {
            errors.rejectValue("start", "tournament.start.past", "Start must be in the future")
        }
    }
}
