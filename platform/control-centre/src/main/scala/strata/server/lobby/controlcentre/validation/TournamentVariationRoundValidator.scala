package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.{TournamentVariationRoundForm, TournamentVariationPayoutForm}
import org.springframework.stereotype.Component

@Component
class TournamentVariationRoundValidator extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[TournamentVariationRoundForm]

    def validate(target: Any, errors: Errors) {
        val variation = target.asInstanceOf[TournamentVariationRoundForm]

        if (variation.getNumber < 1) {
            errors.rejectValue("number", "variation-round.number.invalid", "Number must be 1 or above")
        }

        if (variation.getEndInterval < 0) {
            errors.rejectValue("endInterval", "variation-round.endInterval.invalid", "End Interval must be 0 or above")
        }

        if (variation.getLength < 1) {
            errors.rejectValue("length", "variation-round.length.invalid", "Length must be 1 or above")
        }

        if (variation.getMinimumBalance == null) {
            errors.rejectValue("minimumBalance", "variation-round.minimumBalance.missing",
                "Minimum Balance is required")

        } else if (variation.getMinimumBalance.compare(BigDecimal(0)) < 0) {
            errors.rejectValue("minimumBalance", "variation-round.minimumBalance.invalid",
                "Minimum Balance must be 0 or above")
        }

        if (variation.getDescription == null) {
            errors.rejectValue("description", "variation-round.description.missing",
                "Description is required")
        }
    }
}
