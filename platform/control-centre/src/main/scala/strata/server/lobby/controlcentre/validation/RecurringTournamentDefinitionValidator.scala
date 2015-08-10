package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.RecurringTournamentDefinitionForm
import org.springframework.stereotype.Component

@Component
class RecurringTournamentDefinitionValidator extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[RecurringTournamentDefinitionForm]

    def validate(target: Any, errors: Errors) {
        val definition = target.asInstanceOf[RecurringTournamentDefinitionForm]

        if (definition.tournamentName == null) {
            errors.rejectValue("tournamentName", "definition.tournamentName.missing", "Tournament Name is required")
        }

        if (definition.partnerId == null) {
            errors.rejectValue("partnerId", "definition.partnerId.missing", "Partner ID is required")
        }

        if (definition.variationId == null) {
            errors.rejectValue("variationId", "definition.variationId.missing", "Variation ID is required")
        }

        if (definition.initialSignupTime == null) {
            errors.rejectValue("initialSignupTime", "definition.initialSignupTime.missing",
                "Initial Sign-up Time is required")
        }
    }
}
