package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.SystemMessageForm
import org.springframework.stereotype.Component
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime

@Component
class SystemMessageValidator extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[SystemMessageForm]

    def validate(target: Any, errors: Errors) {
        val systemMessage = target.asInstanceOf[SystemMessageForm]

        if (StringUtils.isBlank(systemMessage.message)) {
            errors.rejectValue("message", "systemMessage.message.missing", "Message is required")
        }

        if (systemMessage.validFrom == null) {
            errors.rejectValue("validFrom", "systemMessage.validFrom.missing", "Valid From is required")
        }

        if (systemMessage.validTo == null) {
            errors.rejectValue("validTo", "systemMessage.validTo.missing", "Valid To is required")

        } else if (systemMessage.validFrom != null
                && systemMessage.validTo.before(systemMessage.validFrom)) {
            errors.rejectValue("validTo", "systemMessage.validTo.inconsistent",
                    "Valid To must be after the Valid From date")

        } else if (systemMessage.validTo.before(new DateTime().toDate)) {
            errors.rejectValue("validTo", "systemMessage.validTo.past", "Valid To must be in the future")
        }
    }
}
