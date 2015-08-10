package com.yazino.web.api;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

/**
 * Common tools for doing validation.
 */
public class ValidationTools {

    private ValidationTools() {
    }
    
    public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String value) {
        if (value == null || StringUtils.isBlank(value)) {
            errors.rejectValue(field, "empty", field  + " must be present");
        }
    }

    public static void rejectUnsupportedValue(Errors errors, String field) {
        errors.rejectValue(field, "unsupported", field + " is not supported");
    }

}
