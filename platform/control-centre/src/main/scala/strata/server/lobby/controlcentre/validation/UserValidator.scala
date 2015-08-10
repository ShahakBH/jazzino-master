package strata.server.lobby.controlcentre.validation

import org.springframework.validation.{Errors, Validator}
import strata.server.lobby.controlcentre.form.UserForm
import org.springframework.stereotype.Component
import org.apache.commons.lang3.StringUtils

@Component
class UserValidator extends Validator {

    def supports(clazz: Class[_]): Boolean = clazz == classOf[UserForm]

    def validate(target: Any, errors: Errors) {
        val user = target.asInstanceOf[UserForm]
        if (StringUtils.isNotBlank(user.password)) {
            if (user.confirmPassword == null || user.password != user.confirmPassword) {
                errors.rejectValue("password", "user.password.mismatch", "Passwords do not match")
            }
        } else if (user.isNew) {
            errors.rejectValue("password", "user.password.missing", "Password is required")
        }
    }
}
