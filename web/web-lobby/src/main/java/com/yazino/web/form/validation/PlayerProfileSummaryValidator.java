package com.yazino.web.form.validation;

import com.yazino.platform.player.PlayerProfileSummary;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class PlayerProfileSummaryValidator implements Validator {

    @Override
    public boolean supports(final Class<?> clazz) {
        return PlayerProfileSummary.class.equals(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final PlayerProfileSummary userProfileInfo = (PlayerProfileSummary) target;
        ValidationTools.validateGender(errors, "gender", userProfileInfo.getGender());
        ValidationTools.validateCountryLength(errors, "country", userProfileInfo.getCountry());
        ValidationTools.validateNotNull(errors, "dateOfBirth", userProfileInfo.getDateOfBirth());
    }
}
