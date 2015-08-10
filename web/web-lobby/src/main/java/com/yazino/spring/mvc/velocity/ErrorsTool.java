package com.yazino.spring.mvc.velocity;

import org.apache.velocity.tools.config.DefaultKey;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.HashSet;
import java.util.Set;

/**
 * Tools to make more sense of an {@link org.springframework.validation.Errors} object.
 */
@DefaultKey("errorsTool")
public class ErrorsTool {

    public Set<String> fieldsWithErrors(Errors errors) {
        Set<String> names = new HashSet<String>();
        for (FieldError error : errors.getFieldErrors()) {
            names.add(error.getField());
        }
        return names;
    }

    public boolean hasFieldErrors(Errors errors) {
        return errors.hasFieldErrors();
    }

    public boolean hasFieldErrors(String field, Errors errors) {
        return errors.hasFieldErrors(field);
    }

    public Set<FieldError> fieldErrors(String field, Errors errors) {
        return new HashSet<FieldError>(errors.getFieldErrors(field));
    }

    public boolean hasGlobalErrors(Errors errors) {
        return errors.hasGlobalErrors();
    }

    public Set<ObjectError> globalErrors(Errors errors) {
        return new HashSet<ObjectError>(errors.getGlobalErrors());
    }


}
