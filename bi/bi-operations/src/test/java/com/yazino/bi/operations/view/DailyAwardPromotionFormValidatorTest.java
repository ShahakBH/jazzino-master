package com.yazino.bi.operations.view;

import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;

public class DailyAwardPromotionFormValidatorTest extends PromotionFormValidatorTest {

    @Override
    protected PromotionFormValidator initPromotionFormValidator() {
        return new DailyAwardPromotionFormValidator();
    }

    @Override
    protected PromotionForm createValidPromotionForm() {
        DailyAwardPromotionForm promotion = new DailyAwardPromotionForm();

//        setCommonValues(promotion);
        promotion.setName("aname");
        promotion.setPlatforms(Arrays.asList(Platform.WEB));
        promotion.setStartDate(new DateTime(2011, 7, 14, 0, 0, 0, 0));
        promotion.setStartHour(12);
        promotion.setStartMinute(30);
        promotion.setEndHour(10);
        promotion.setEndMinute(11);
        promotion.setEndDate(new DateTime(2011, 7, 21, 0, 0, 0, 0));

        promotion.setMaximumRewards(10);
        promotion.setRewardChips(2500);
        return promotion;
    }

    @Test
    public void supportsDailyAwardPromotionForm() {
        assertTrue(underTest.supports(DailyAwardPromotionForm.class));
    }

    @Test
    public void awardChipsIsRequired() {
        DailyAwardPromotionForm promotion = (DailyAwardPromotionForm) createValidPromotionForm();
        promotion.setRewardChips(null);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("rewardChips"));
    }

    @Test
    public void awardChipsMustBeGreaterThanZero() {
        DailyAwardPromotionForm promotion = (DailyAwardPromotionForm) createValidPromotionForm();
        promotion.setRewardChips(0);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("rewardChips"));
    }

}
