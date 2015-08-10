package com.yazino.bi.operations.view;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service("dailyAwardPromotionFormValidator")
public class DailyAwardPromotionFormValidator extends PromotionFormValidator {

    @Override
    public boolean supports(final Class<?> aClass) {
        return DailyAwardPromotionForm.class.isAssignableFrom(aClass);
    }

    @Override
    public void validate(final Object form, final Errors errors) {
        super.validate(form, errors);
        validateDailyAwardFields(form, errors);
    }

    private void validateDailyAwardFields(final Object o, final Errors errors) {
        final DailyAwardPromotionForm dailyAwardForm = (DailyAwardPromotionForm) o;
        if (!errors.hasFieldErrors("rewardChips") && (dailyAwardForm.getRewardChips() == null
                || dailyAwardForm.getRewardChips() <= 0)) {
            errors.rejectValue("rewardChips", "zerovalue");
        }
    }




}
