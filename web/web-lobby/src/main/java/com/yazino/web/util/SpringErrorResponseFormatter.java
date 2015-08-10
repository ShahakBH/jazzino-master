package com.yazino.web.util;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.*;

/**
 * A utility class to turn Spring binding results into JSON.
 * <p/>
 * This is a side effect of ye olde days when people implemented JSON via Velocity views, and pretty
 * raw spring binding conversion. It's nasty, but such are the joys of backwards compatibility.
 * <p/>
 * Please don't use this for new stuff. Write a sensible format that isn't tied to Spring.
 */
@Service
public class SpringErrorResponseFormatter {

    @SuppressWarnings("unchecked")
    public Map<String, Object> toJson(final BindingResult bindingResult) {
        if (bindingResult == null) {
            return Collections.emptyMap();
        }

        final Map<String, Object> json = new HashMap<>();
        if (bindingResult.hasGlobalErrors()) {
            final List<Map<String, Object>> messages = new ArrayList<>();
            for (ObjectError objectError : bindingResult.getGlobalErrors()) {
                messages.add(message(objectError.getCode(), objectError.getDefaultMessage()));
            }
            json.put("globalMessages", messages);
        }
        if (bindingResult.hasFieldErrors()) {
            final Map<String, List<Map<String, Object>>> fieldErrors = new HashMap<>();
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                List<Map<String, Object>> errorsForField = fieldErrors.get(fieldError.getField());
                if (errorsForField == null) {
                    errorsForField = new ArrayList<>();
                    fieldErrors.put(fieldError.getField(), errorsForField);
                }
                errorsForField.add(message(fieldError.getCode(), fieldError.getDefaultMessage()));
            }
            json.put("fieldErrors", fieldErrors);
        }
        return json;
    }

    private Map<String, Object> message(final String code,
                                        final String message) {
        final Map<String, Object> json = new HashMap<>();
        json.put("code", code);
        json.put("message", message);
        return json;
    }

}
