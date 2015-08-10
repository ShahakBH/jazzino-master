package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.{TrophyForm, SystemMessageForm}
import org.springframework.stereotype.Component
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime

@Component
class TrophyValidator extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[TrophyForm]

    def validate(target: Any, errors: Errors) {
        val trophy = target.asInstanceOf[TrophyForm]

        if (StringUtils.isBlank(trophy.name)) {
            errors.rejectValue("name", "trophy.name.missing", "Name is required")
        }

        if (StringUtils.isBlank(trophy.gameType)) {
            errors.rejectValue("gameType", "trophy.gameType.missing", "Game Type is required")
        }

        if (StringUtils.isBlank(trophy.image)) {
            errors.rejectValue("image", "trophy.image.missing", "Image is required")
        }

        if (StringUtils.isBlank(trophy.message)) {
            errors.rejectValue("message", "trophy.message.missing", "Message is required")
        }

        if (StringUtils.isBlank(trophy.shortDescription)) {
            errors.rejectValue("shortDescription", "trophy.shortDescription.missing",
                "Short Description is required")
        }
    }
}
