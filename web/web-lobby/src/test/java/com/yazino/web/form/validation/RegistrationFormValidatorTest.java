package com.yazino.web.form.validation;

import com.yazino.web.form.MobileRegistrationForm;
import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests the {@link com.yazino.web.form.validation.RegistrationFormValidator} class.
 */
public class RegistrationFormValidatorTest {

    private final RegistrationFormValidator validator = new RegistrationFormValidator();
    private final Errors errors = new MapBindingResult(new HashMap(), "registration");
    private final MobileRegistrationForm form = validForm();

    @Test
    public void onlySupportsRegistrationFormClasses() throws Exception {
        assertFalse(validator.supports(Integer.class));
        assertTrue(validator.supports(MobileRegistrationForm.class));
    }

    @Test
    public void displayName_nullValue_failsValidation() throws Exception {
        validateDisplayNameValue(null, true);
    }

    @Test
    public void displayName_emptyValue_failsValidation() throws Exception {
        validateDisplayNameValue("", true);
    }

    @Test
    public void displayName_whitespaceValue_failsValidation() throws Exception {
        validateDisplayNameValue("   ", true);
    }

    @Test
    public void displayName_quotesValue_failsValidation() throws Exception {
        validateDisplayNameValue("\"jim\"", true);
    }

    @Test
    public void displayName_invalidValueTooShort_failsValidation() throws Exception {
        validateDisplayNameValue("Te", true);
    }

    @Test
    public void displayName_invalidValueTooLong_failsValidation() throws Exception {
        validateDisplayNameValue("12345678901234567890abcdef", true);
    }

    @Test
    public void displayName_okValue_passesValidation() throws Exception {
        validateDisplayNameValue("Test", false);
    }

    private void validateDisplayNameValue(String value, boolean hasFieldErrors) {
        form.setDisplayName(value);
        if (hasFieldErrors) {
            validateAndAssertFailedFields(form, "displayName");
        } else {
            validateAndAssertFailedFields(form);
        }
    }

    @Test
    public void password_nullValue_failsValidation() throws Exception {
        validatePasswordValue(null, true);
    }

    @Test
    public void password_emptyValue_failsValidation() throws Exception {
        validatePasswordValue("", true);
    }

    @Test
    public void password_whitespaceValue_failsValidation() throws Exception {
        validatePasswordValue("   ", true);
    }

    @Test
    public void password_invalidValueTooShort_failsValidation() throws Exception {
        validatePasswordValue("foo0", true);
    }

    @Test
    public void password_invalidValueTooLong_failsValidation() throws Exception {
        validatePasswordValue("12345678901234567890abcdef", true);
        validatePasswordValue("12345678901234567890", true);
    }

    private void validatePasswordValue(String value, boolean passwordErrors) {
        form.setPassword(value);
        List<String> errors = new ArrayList<String>();
        if (passwordErrors) {
            errors.add("password");
        }
        validateAndAssertFailedFields(form, errors.toArray(new String[errors.size()]));
    }


    @Test
    public void emailAddress_nullValue_failsValidation() throws Exception {
        validateEmailAddressValue(null, true);
    }

    @Test
    public void emailAddress_emptyValue_failsValidation() throws Exception {
        validateEmailAddressValue("", true);
    }

    @Test
    public void emailAddress_whitespaceValue_failsValidation() throws Exception {
        validateEmailAddressValue("   ", true);
    }

    @Test
    public void emailAddress_invalidValue1_failsValidation() throws Exception {
        validateEmailAddressValue("goo@bar", true);
    }

    @Test
    public void emailAddress_invalidValue2_failsValidation() throws Exception {
        validateEmailAddressValue("goobar.com", true);
    }

    @Test
    public void emailAddress_invalidValueTooLong_failsValidation() throws Exception {
        validateEmailAddressValue("12345678901234567890123456789012345678901234567890@bar.com", true);
    }

    private void validateEmailAddressValue(String value, boolean emailErrors) {
        form.setEmail(value);
        List<String> errors = new ArrayList<String>();
        if (emailErrors) {
            errors.add("email");
        }
        validateAndAssertFailedFields(form, errors.toArray(new String[errors.size()]));
    }

    @Test
    public void termsAndConditions_okValue_failsValidation() throws Exception {
        validateTermsAndConditionsValue(false, true);
    }

    @Test
    public void termsAndConditions_trueValue_passesValidation() throws Exception {
        validateTermsAndConditionsValue(true, false);
    }

    private void validateTermsAndConditionsValue(boolean value, boolean hasFieldErrors) {
        form.setTermsAndConditions(value);
        if (hasFieldErrors) {
            validateAndAssertFailedFields(form, "termsAndConditions");
        } else {
            validateAndAssertFailedFields(form);
        }
    }

    private MobileRegistrationForm validForm() {
        MobileRegistrationForm form = new MobileRegistrationForm();
        form.setTermsAndConditions(true);
        form.setDisplayName("Test");
        form.setEmail("a@b.com");
        form.setPassword("foobar");
        return form;
    }

    private void validateAndAssertFailedFields(MobileRegistrationForm form, String... failedFields) {
        validator.validate(form, errors);
        assertEquals(failedFields.length, errors.getFieldErrorCount());
        for (String failedField : failedFields) {
            assertTrue(errors.hasFieldErrors(failedField));
        }
    }

}