package com.yazino.web.form.validation;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validates the length of a String.
 * Handles nulls by treating them as empty strings. Trims all strings before validation.
 */
public class StringLengthValidator implements Validator {
    private static final Logger LOG = LoggerFactory.getLogger(StringLengthValidator.class);

    private final int minimumLength;
    private final int maximumLength;

    public StringLengthValidator(final int minimumLength, final int maximumLength) {
        Validate.isTrue(minimumLength >= 0);
        Validate.isTrue(maximumLength >= minimumLength);
        this.minimumLength = minimumLength;
        this.maximumLength = maximumLength;
    }

    @Override
    public boolean supports(final Class clazz) {
        return String.class.equals(clazz);
    }

    @Override
    public void validate(final Object target,
                         final Errors errors) {
        String value = (String) target;
        if (StringUtils.isBlank(value)) {
            value = "";
        }

        final int length = value.trim().length();
        if (length < minimumLength) {
            errors.reject("length.tooShort", "Value too short");
        } else if (length > maximumLength) {
            errors.reject("length.tooLong", "Value too long");
        }
    }

    public int getMinimumLength() {
        return minimumLength;
    }

    public int getMaximumLength() {
        return maximumLength;
    }

}
