package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.TournamentVariationPayoutForm
import org.springframework.stereotype.Component

@Component
class TournamentVariationPayoutValidator extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[TournamentVariationPayoutForm]

    def validate(target: Any, errors: Errors) {
        val variation = target.asInstanceOf[TournamentVariationPayoutForm]

        if (variation.getRank < 1) {
            errors.rejectValue("rank", "variation-payout.rank.invalid", "Rank must be 1 or above")
        }

        if (variation.getPayout == null) {
            errors.rejectValue("payout", "variation-payout.payout.missing", "Payout is required")

        } else if (variation.getPayout.compare(BigDecimal(0)) < 0
            || variation.getPayout.compare(BigDecimal(1)) > 0) {
            errors.rejectValue("payout", "variation-payout.payout.invalid", "Payout must be between 0 and 1")
        }
    }
}
