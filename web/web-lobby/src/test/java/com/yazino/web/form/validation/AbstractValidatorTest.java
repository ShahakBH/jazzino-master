package com.yazino.web.form.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractValidatorTest {

    @Mock
    protected Errors errors;

    @Test
    public void shouldSupportCorrectClass() {
        assertTrue(getUnderTest().supports(getSupportedClass()));
    }

    @Test
    public void shouldNotSupportOtherClasses() {
        assertFalse(getUnderTest().supports(String.class));
    }

    protected void assertNoErrors(Object objectToValidate) {
        getUnderTest().validate(objectToValidate, errors);
        verify(errors, never()).rejectValue(anyString(), anyString(), anyString());
    }

    protected void assertErrorCodeEmpty(String fieldName, Object objectToValidate) {
        getUnderTest().validate(objectToValidate, errors);
        verify(errors, atLeastOnce()).rejectValue(fieldName, ValidationTools.ERROR_CODE_EMPTY, ValidationTools.DEFAULT_TEXT_EMPTY_VALUE);
    }

    protected void assertErrorCodeInvalid(String fieldName, Object objectToValidate) {
        getUnderTest().validate(objectToValidate, errors);

        verify(errors).rejectValue(fieldName, ValidationTools.ERROR_CODE_INVALID, ValidationTools.DEFAULT_TEXT_INVALID_VALUE);
    }

    protected void assertErrorCodeNotAlphaNumeric(String fieldName, Object objectToValidate) {
        getUnderTest().validate(objectToValidate, errors);

        verify(errors).rejectValue(fieldName, ValidationTools.ERROR_CODE_NOT_ALPHANUMERIC, ValidationTools.DEFAULT_TEXT_NOT_ALPHANUMERIC);
    }

    protected void assertErrorCodeValuesDonNotMatch(String fieldName, Object objectToValidate) {
        getUnderTest().validate(objectToValidate, errors);

        verify(errors).rejectValue(fieldName, ValidationTools.ERROR_CODE_NON_MATCHING, ValidationTools.DEFAULT_TEXT_NON_MATCHING);
    }

    protected abstract Validator getUnderTest();

    protected abstract Class getSupportedClass();
}
