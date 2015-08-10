package com.yazino.bi.operations.view;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import static strata.server.lobby.api.promotion.Promotion.*;

public abstract class PromotionFormValidator implements Validator {

    static final int PROMOTION_NAME_MAX_LENGTH = 50;

    public void validate(final Object object, final Errors errors) {
        final PromotionForm form = (PromotionForm) object;
        validateType(errors, form);
        validateName(errors, form);
        validatePlatforms(errors, form);
        validateDates(errors, form);
        validateSeed(errors, form);
        validateControlGroupPercentage(errors, form);
        validateMaximumRewards(errors, form.getMaximumRewards());
        validateDailyAwardPopupImages(errors, form.getDailyAwardPopupImagesForm());
    }

    private void validateMaximumRewards(final Errors errors, final Integer maxRewards) {
        if (!errors.hasFieldErrors("maximumRewards") && (maxRewards == null || maxRewards <= 0)) {
            errors.rejectValue("maximumRewards", "zerovalue");
        }
    }

    private void validateSeed(final Errors errors, final PromotionForm promotion) {
        if (promotion.getSeed() < MIN_SEED_VALUE || promotion.getSeed() > MAX_SEED_VALUE) {
            errors.rejectValue("seed", "seed.range.error",
                    String.format("seed must lie between %s and %s", MIN_SEED_VALUE, MAX_SEED_VALUE));
        }
    }

    private void validateControlGroupPercentage(final Errors errors, final PromotionForm promotion) {
        if (!errors.hasFieldErrors("controlGroupPercentage")
                && promotion.getControlGroupPercentage() < MIN_CONTROL_GROUP_PERCENTAGE
                || promotion.getControlGroupPercentage() > MAX_CONTROL_GROUP_PERCENTAGE) {
            errors.rejectValue("controlGroupPercentage", "controlGroupPercentage.range.error",
                    String.format("Control group percentage is an integer between %s and %s",
                            MIN_CONTROL_GROUP_PERCENTAGE, MAX_CONTROL_GROUP_PERCENTAGE));
        }
    }

    private void validateDates(final Errors errors, final PromotionForm promotion) {
        if (promotion.getStartDate() == null) {
            errors.rejectValue("startDate", "empty");
        }
        if (promotion.getEndDate() == null) {
            errors.rejectValue("endDate", "empty");
        }
        if (!errors.hasFieldErrors("startDate") && !errors.hasFieldErrors("endDate")) {
            final DateTime startDateWithTime = promotion.getStartDate().withHourOfDay(promotion.getStartHour())
                    .withMinuteOfHour(promotion.getStartMinute());
            final DateTime endDateWithTime = promotion.getEndDate().withHourOfDay(promotion.getEndHour())
                    .withMinuteOfHour(promotion.getEndMinute());
            if (endDateWithTime.isBefore(startDateWithTime)) {
                errors.rejectValue("startDate", "startDate.after.endDate");
            }
        }
    }

    private void validateName(final Errors errors, final PromotionForm promotion) {
        if (StringUtils.isBlank(promotion.getName())) {
            errors.rejectValue("name", "empty");
        }
        if (promotion.getName().length() > PROMOTION_NAME_MAX_LENGTH) {
            errors.rejectValue("name", "name.length");
        }
    }

    private void validatePlatforms(final Errors errors, final PromotionForm promotion) {
        if (promotion.getPlatforms().isEmpty()) {
            errors.rejectValue("platforms", "target.client.required",
                    "Choose at lease one platform for this promotion");
        }
    }

    private void validateType(final Errors errors, final PromotionForm promotion) {
        if (promotion.getPromotionType() == null) {
            errors.rejectValue("promotionType", "empty", "type must be set");
        }
    }

    private void validateDailyAwardPopupImages(final Errors errors,
                                               final DailyAwardPopupImagesForm dailyAwardPopupImagesForm) {
        validateMainImage(errors, dailyAwardPopupImagesForm);
        validateSecondaryImage(errors, dailyAwardPopupImagesForm);
        validateIosImage(errors, dailyAwardPopupImagesForm);
    }

    private void validateSecondaryImage(final Errors errors,
                                        final DailyAwardPopupImagesForm dailyAwardPopupImagesForm) {
        final ImageForm secondaryImage = dailyAwardPopupImagesForm.getSecondaryImage();
        if ("upload".equals(secondaryImage.getImageType())) {
            final MultipartFile secImageFile = secondaryImage.getImageFile();
            if (secImageFile == null || StringUtils.isBlank(secImageFile.getOriginalFilename())) {
                errors.rejectValue("secondaryImage.imageFile", "upload.image.required", "Upload secondary image");
                secondaryImage.setImageUrl(null);
            }
        }
    }

    private void validateMainImage(final Errors errors,
                                   final DailyAwardPopupImagesForm dailyAwardPopupImagesForm) {
        final ImageForm mainImage = dailyAwardPopupImagesForm.getMainImage();
        if ("upload".equals(mainImage.getImageType())) {
            final MultipartFile mainImageFile = mainImage.getImageFile();
            if (mainImageFile == null || StringUtils.isBlank(mainImageFile.getOriginalFilename())) {
                errors.rejectValue("mainImage.imageFile", "upload.image.required", "Upload main image");
                mainImage.setImageUrl(null);
            }
        }
    }

    private void validateIosImage(final Errors errors,
                                  final DailyAwardPopupImagesForm dailyAwardPopupImagesForm) {
        final ImageForm iosImage = dailyAwardPopupImagesForm.getIosImage();
        if ("upload".equals(iosImage.getImageType())) {
            final MultipartFile iosImageFile = iosImage.getImageFile();
            if (iosImageFile == null || StringUtils.isBlank(iosImageFile.getOriginalFilename())) {
                errors.rejectValue("iosImage.imageFile", "upload.image.required", "Upload iOS image");
                iosImage.setImageUrl(null);
            }
        }
    }

}
