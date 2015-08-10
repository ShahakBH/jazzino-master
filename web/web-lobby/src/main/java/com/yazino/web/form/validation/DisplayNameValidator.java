package com.yazino.web.form.validation;


import com.yazino.web.domain.DisplayName;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static com.yazino.web.form.RegistrationFormConstants.DISPLAY_NAME;

public class DisplayNameValidator implements Validator {
    @Override
    public boolean supports(final Class<?> clazz) {
        return DisplayName.class.equals(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final DisplayName form = (DisplayName) target;
        ValidationTools.validateDisplayName(errors, DISPLAY_NAME, form.getDisplayName());
    }
}
