package com.yazino.bi.operations.view;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class PromotionFormValidatorTest {

    private static final int PROMOTION_NAME_MAX_LENGTH = 50;

    protected PromotionFormValidator underTest;

    protected abstract PromotionFormValidator initPromotionFormValidator();

    @Before
    public void init() {
        underTest = initPromotionFormValidator();
    }

    @Test
    public void validateWithValidPromotionAddsNoErrors() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertFalse(errors.hasErrors());
    }

    protected abstract PromotionForm createValidPromotionForm();

    @Test
    public void validateAddsErrorForNameTooLong() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setName(StringUtils.leftPad("", PROMOTION_NAME_MAX_LENGTH + 1, 'x'));
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("name"));
    }
//
    @Test
    public void validateAddsErrorForNoPlatforms() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setPlatforms(Collections.<Platform>emptyList());
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("platforms"));
    }
//
    @Test
    public void validateAddsErrorsForNullDates() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setStartDate(null);
        promotion.setEndDate(null);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("startDate"));
        assertTrue(errors.hasFieldErrors("endDate"));
    }

    @Test
    public void validateAddsErrorWhenStartDateAfterEndDate() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setStartDate(new DateTime(2011, 7, 14, 14, 30, 0, 0));
        promotion.setEndDate(promotion.getStartDate().minusDays(1));
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("startDate"));
    }

    @Test
    public void seedMustBeNotBeLessThanZero() {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setSeed(-1);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("seed"));
    }

    @Test
    public void seedMustNotExceed100() {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setSeed(101);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("seed"));
    }

    @Test
    public void controlGroupPercentageMustNotBeLessThanZero() {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setControlGroupPercentage(-1);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("controlGroupPercentage"));
    }

    @Test
    public void controlGroupPercentageMustNotExceed100() {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setControlGroupPercentage(101);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("controlGroupPercentage"));
    }

    @Test
    public void maximiumRewardsMustBeGreaterThanZero() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.setMaximumRewards(0);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("maximumRewards"));
    }


    @Test
    public void ifDefaultMainImageIsUsedThenNoMainImageErrorsReported() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.getMainImage().setImageType("default");
        promotion.getMainImage().setImageUrl("");
        promotion.getMainImage().setImageFile(null);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    public void ifUploadMainImageIsUsedAndFilenameIsEmptyThenMainImageReported() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.getMainImage().setImageType("upload");
        promotion.getMainImage().setImageFile(new MockMultipartFile("main", "", null, new byte[]{}));
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("mainImage.imageFile"));
    }

    @Test
    public void ifDefaultSecImageIsUsedThenNoSecImageErrorsReported() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.getSecondaryImage().setImageType("default");
        promotion.getSecondaryImage().setImageUrl("");
        promotion.getSecondaryImage().setImageFile(null);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    public void ifUploadSecImageIsUsedAndFilenameIsEmptyThenSecImageReported() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.getSecondaryImage().setImageType("upload");
        promotion.getSecondaryImage().setImageFile(new MockMultipartFile("sec", "", null, new byte[]{}));
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("secondaryImage.imageFile"));
    }

    @Test
    public void ifDefaultIosImageIsUsedThenNoIosImageErrorsReported() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.getIosImage().setImageType("default");
        promotion.getIosImage().setImageUrl("");
        promotion.getIosImage().setImageFile(null);
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    public void ifUploadIosImageIsUsedAndFilenameIsEmptyThenIosImageReported() throws Exception {
        PromotionForm promotion = createValidPromotionForm();
        promotion.getIosImage().setImageType("upload");
        promotion.getIosImage().setImageFile(new MockMultipartFile("ios", "", null, new byte[]{}));
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("iosImage.imageFile"));
    }
//
//    private PromotionForm createValidPromotionForm() {
//
//        PromotionForm form = initPromotionForm();
//        form.setPromotionType(PromotionType.BUY_CHIPS);
//        form.setName("aname");
//        form.setPlatforms(Arrays.asList(new Platform[]{Platform.WEB}));
//        form.setMaximumRewards(4);
//        form.setStartDate(new DateTime(2011, 7, 14, 0, 0, 0, 0));
//        form.setStartHour(12);
//        form.setStartMinute(30);
//        form.setEndHour(10);
//        form.setEndMinute(11);
//        form.setEndDate(new DateTime(2011, 7, 21, 0, 0, 0, 0));
//
//        MockMultipartFile main = new MockMultipartFile("main", "main", null, new byte[]{});
//        form.getMainImage().setImageFile(main);
//        MockMultipartFile sec = new MockMultipartFile("sec", "sec", null, new byte[]{});
//        form.getSecondaryImage().setImageFile(main);
//        form.getIosImage().setImageFile(main);
//
//        return form;
//    }
//
//    private void setCommonValues(PromotionForm promotion) {
//        promotion.setName("aname");
//        promotion.setPlatforms(Arrays.asList(new Platform[]{Platform.WEB}));
//        promotion.setStartDate(new DateTime(2011, 7, 14, 0, 0, 0, 0));
//        promotion.setStartHour(12);
//        promotion.setStartMinute(30);
//        promotion.setEndHour(10);
//        promotion.setEndMinute(11);
//        promotion.setEndDate(new DateTime(2011, 7, 21, 0, 0, 0, 0));
//    }
//
//

}
