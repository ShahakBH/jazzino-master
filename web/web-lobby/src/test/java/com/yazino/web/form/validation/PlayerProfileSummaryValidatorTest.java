package com.yazino.web.form.validation;

import com.yazino.platform.player.PlayerProfileSummary;
import com.yazino.web.domain.PlayerProfileSummaryTestBuilder;
import org.junit.Test;
import org.springframework.validation.Validator;

import static org.mockito.Mockito.verify;

public class PlayerProfileSummaryValidatorTest extends AbstractValidatorTest {

    private final PlayerProfileSummaryValidator underTest = new PlayerProfileSummaryValidator();

    @Test
    public void shouldReturnErrorForInvalidGender() {
        PlayerProfileSummary userProfileInfo = PlayerProfileSummaryTestBuilder.create().withGender("FF").build();
        assertErrorCodeInvalid("gender", userProfileInfo);
    }

    @Test
    public void shouldReturnErrorForNoGender() {
        PlayerProfileSummary userProfileInfo = PlayerProfileSummaryTestBuilder.create().withGender("").build();
        assertErrorCodeEmpty("gender", userProfileInfo);
    }

    @Test
    public void shouldReturnErrorForTooLongCountry() {
        PlayerProfileSummary userProfileInfo = PlayerProfileSummaryTestBuilder.create().withCountry("USSR").build();

        getUnderTest().validate(userProfileInfo, errors);

        verify(errors).rejectValue("country", ValidationTools.ERROR_CODE_LENGTH, "Must be between 1 and 3 characters");
    }

    @Test
    public void shouldReturnErrorIfNoDate() {
        PlayerProfileSummary userProfileInfo = PlayerProfileSummaryTestBuilder.create().withDateOfBirth(null).build();
        assertErrorCodeEmpty("dateOfBirth", userProfileInfo);
    }

    @Test
    public void shouldReturnNoErrorsWithPerfectObject() {
        assertNoErrors(PlayerProfileSummaryTestBuilder.create().build());
    }

    @Override
    protected Validator getUnderTest() {
        return underTest;
    }

    @Override
    protected Class getSupportedClass() {
        return PlayerProfileSummary.class;
    }
}
