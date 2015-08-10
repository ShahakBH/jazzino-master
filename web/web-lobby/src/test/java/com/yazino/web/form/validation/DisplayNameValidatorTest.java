package com.yazino.web.form.validation;

import com.yazino.web.domain.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.Validator;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DisplayNameValidatorTest extends AbstractValidatorTest {

    DisplayNameValidator underTest = new DisplayNameValidator();

    @Test
    public void shouldReturnErrorsOnInvalidUserName() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            stringBuilder.append("MyName");
        }
        DisplayName displayName = new DisplayName(stringBuilder.toString());

        getUnderTest().validate(displayName, errors);

        verify(errors).rejectValue("displayName", ValidationTools.ERROR_CODE_LENGTH, "Must be between 3 and 10 characters");
    }

    @Test
    public void shouldReturnNoErrorsWithPerfectObject() {
        assertNoErrors(new DisplayName("My Name"));
    }

    @Test
    public void validationShouldFailOnNonAlphanumericValues() {
        assertErrorCodeNotAlphaNumeric("displayName", new DisplayName("\"james\""));
    }

    @Override
    protected Validator getUnderTest() {
        return underTest;
    }

    @Override
    protected Class getSupportedClass() {
        return DisplayName.class;
    }


}
