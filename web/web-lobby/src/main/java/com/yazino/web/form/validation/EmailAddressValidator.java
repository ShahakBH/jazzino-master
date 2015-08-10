package com.yazino.web.form.validation;

import com.yazino.web.domain.EmailAddress;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class EmailAddressValidator implements Validator {

    @Override
    public boolean supports(final Class<?> clazz) {
        return EmailAddress.class.equals(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final EmailAddress form = (EmailAddress) target;
        ValidationTools.validateEmailAddress(errors, "emailAddress", form.getEmailAddress());
    }
}
