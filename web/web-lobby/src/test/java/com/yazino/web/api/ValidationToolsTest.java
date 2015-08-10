package com.yazino.web.api;

import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ValidationToolsTest {

    private final Errors errors = new MapBindingResult(new HashMap<Object, Object>(), "");

    @Test
    public void shouldRejectStringIfNull() throws Exception {
        ValidationTools.rejectIfEmptyOrWhitespace(errors, "foo", null);
        assertHasEmptyFieldError(errors, "foo");
    }

    @Test
    public void shouldRejectStringIfEmpty() throws Exception {
        ValidationTools.rejectIfEmptyOrWhitespace(errors, "bar", "");
        assertHasEmptyFieldError(errors, "bar");
    }

    @Test
    public void shouldRejectStringIfWhitespace() throws Exception {
        ValidationTools.rejectIfEmptyOrWhitespace(errors, "foobar", "     ");
        assertHasEmptyFieldError(errors, "foobar");
    }

    @Test
    public void shouldAllowStringWhichStartsWithWhitespace() throws Exception {
        ValidationTools.rejectIfEmptyOrWhitespace(errors, "foobar", "     fdsfds");
        assertFalse(errors.hasFieldErrors("foobar"));
    }

    @Test
    public void shouldAllowStringWhichEndsWithWhitespace() throws Exception {
        ValidationTools.rejectIfEmptyOrWhitespace(errors, "foobar", "firestarted   ");
        assertFalse(errors.hasFieldErrors("foobar"));
    }

    @Test
    public void shouldAllowString() throws Exception {
        ValidationTools.rejectIfEmptyOrWhitespace(errors, "foobar", "bar humbug");
        assertFalse(errors.hasFieldErrors("foobar"));
    }

    @Test
    public void shouldRejectUnsupportedValue() throws Exception {
        ValidationTools.rejectUnsupportedValue(errors, "fooo");
        List<FieldError> fieldErrors = errors.getFieldErrors("fooo");
        assertEquals(1, fieldErrors.size());
        FieldError fieldError = fieldErrors.get(0);
        assertEquals("unsupported", fieldError.getCode());
        assertEquals("fooo is not supported", fieldError.getDefaultMessage());

    }

    private static void assertHasEmptyFieldError(Errors errors, String fieldName) {
        List<FieldError> fieldErrors = errors.getFieldErrors(fieldName);
        assertEquals(1, fieldErrors.size());
        FieldError fieldError = fieldErrors.get(0);
        assertEquals("empty", fieldError.getCode());
        assertEquals(fieldName + " must be present", fieldError.getDefaultMessage());
    }
}
