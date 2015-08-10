package com.yazino.web.form.validation;

import com.yazino.web.form.MobileRegistrationForm;
import com.yazino.web.form.RegistrationForm;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static com.yazino.web.form.RegistrationFormConstants.DISPLAY_NAME;

/**
 * This class validates a {@link RegistrationFormValidator} object.
 */
public class RegistrationFormValidator implements Validator {

    @Override
    public boolean supports(final Class clazz) {
        return MobileRegistrationForm.class.equals(clazz);
    }

    @Override
    public void validate(final Object target,
                         final Errors errors) {
        final RegistrationForm form = (RegistrationForm) target;

        ValidationTools.validateDisplayName(errors, DISPLAY_NAME, form.getDisplayName());

        ValidationTools.validatePassword(errors, "password", form.getPassword());

        ValidationTools.validateEmailAddress(errors, "email", form.getEmail()); //can't use emailAddress

        ValidationTools.validateTrue(errors, "termsAndConditions", form.getTermsAndConditions());

        //no validation for OptIn
    }


}
