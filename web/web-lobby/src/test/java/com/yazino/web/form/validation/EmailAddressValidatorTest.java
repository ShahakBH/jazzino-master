package com.yazino.web.form.validation;

import com.yazino.web.domain.EmailAddress;
import org.junit.Test;
import org.springframework.validation.Validator;

import static org.mockito.Mockito.verify;

public class EmailAddressValidatorTest extends AbstractValidatorTest {
    private final EmailAddressValidator underTest = new EmailAddressValidator();

    @Override
    protected Validator getUnderTest() {
        return underTest;
    }

    @Override
    protected Class getSupportedClass() {
        return EmailAddress.class;
    }

    @Test
    public void shouldReturnNoErrorsWithPerfectObject() {
        assertNoErrors(new EmailAddress("something@something.com"));
    }

    @Test
    public void shouldReturnErrorForShortEmailAddress() {
        getUnderTest().validate(new EmailAddress("a@a"), errors);

        verify(errors).rejectValue("emailAddress", ValidationTools.ERROR_CODE_LENGTH, "Must be between 5 and 50 characters");
    }

    @Test
    public void shouldReturnErrorForInvalidEmailAddress() {
        assertErrorCodeInvalid("emailAddress", new EmailAddress("invalid"));
    }

}
