package com.yazino.bi.operations.view;

import com.yazino.platform.Platform;
import strata.server.lobby.api.promotion.*;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.view.ChipPackageHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PromotionFormFactory {

    private PromotionFormFactory() {
    }

    public static final int DEFAULT_END_HOUR = 23;
    public static final int DEFAULT_END_MINUTE = 59;
    public static final int DEFAULT_START_HOUR = 0;
    public static final int DEFAULT_START_MINUTE = 0;

    public static PromotionForm createDefaultForm(final PromotionType type,
                                                  final DailyAwardConfig defaultDailyAwardConfiguration/*,
                                                  final Map<Platform, List<ChipPackage>> defaultChipPackages*/) {
        switch (type) {
            case DAILY_AWARD:
                return createDailyAwardForm(defaultDailyAwardConfiguration);
            case BUY_CHIPS:
                return createBuyChipsForm();
            default:
                return null;
        }
    }

    private static PromotionForm createBuyChipsForm(/*Map<Platform, List<ChipPackage>> defaultChipPackages*/) {
        final BuyChipsPromotionForm form = new BuyChipsPromotionForm(); //defaultChipPackages);
        initCommonDefaultAttrs(form);
        form.getMainImage().setImageType("default");
        form.getSecondaryImage().setImageType("default");
        form.getIosImage().setImageType("default");
        form.getAndroidImage().setImageType("default");
        return form;
    }

    private static PromotionForm createDailyAwardForm(DailyAwardConfig defaultDailyAwardConfiguration) {
        final DailyAwardPromotionForm form = new DailyAwardPromotionForm();
        initCommonDefaultAttrs(form);
        form.getMainImage().setImageUrl(defaultDailyAwardConfiguration.getMainImage());
        form.getSecondaryImage().setImageUrl(defaultDailyAwardConfiguration.getSecondaryImage());
        form.getIosImage().setImageUrl(defaultDailyAwardConfiguration.getIosImage());

        form.getMainImage().setImageType("current");
        form.getSecondaryImage().setImageType("current");
        form.getIosImage().setImageType("current");

        return form;
    }

    public static PromotionForm createPromotionForm(final Promotion promotion) {
        final PromotionType promotionType = promotion.getPromotionType();
        if (promotionType == PromotionType.DAILY_AWARD || promotionType.isProgressive()) {
            return new DailyAwardPromotionForm((DailyAwardPromotion) promotion);
        }
        if (promotionType == PromotionType.BUY_CHIPS) {
            return new BuyChipsPromotionForm((BuyChipsPromotion) promotion);
        }

        throw new IllegalArgumentException("Attempting to creating PromotionForm with unknown type argument: "
                + promotionType);
    }

    private static void initCommonDefaultAttrs(final PromotionForm promotionForm) {
        final List<Platform> platforms = new ArrayList<Platform>(); //Arrays.asList(Platform.WEB, Platform.IOS, Platform.ANDROID, Platform.FACEBOOK_CANVAS);
        promotionForm.setAllPlayers("ALL");
        promotionForm.setStartHour(DEFAULT_START_HOUR);
        promotionForm.setStartMinute(DEFAULT_START_MINUTE);
        promotionForm.setEndHour(DEFAULT_END_HOUR);
        promotionForm.setEndMinute(DEFAULT_END_MINUTE);
        promotionForm.setControlGroupFunction(ControlGroupFunctionType.EXTERNAL_ID);
        promotionForm.setPlatforms(platforms);
    }


    public static void applyOverridesFromPromotionToForm(BuyChipsPromotion promotion, BuyChipsPromotionForm form, Map<Platform,
            List<ChipPackage>> defaultChipPackages) {
        final Map<Platform, BigDecimal[]> defaultChipAmounts = ChipPackageHelper.getDefaultChipAmountsForAllPlatforms(defaultChipPackages);
        PromotionConfiguration configuration = promotion.getConfiguration();
        for (Map.Entry<Platform, BigDecimal[]> platformEntry : defaultChipAmounts.entrySet()) {
            for (BigDecimal defaultAmount : platformEntry.getValue()) {
                BigDecimal override = configuration.getOverriddenChipAmountFormPlatformAndPackage(platformEntry.getKey(), defaultAmount);
                if (override != null) {
                    form.getPlatformToDefaultToOverriddenChipAmounts().get(platformEntry.getKey()).put(defaultAmount, override);
                }
            }
        }
    }

    public static void applyDefaultsPackages(Map<Platform, List<ChipPackage>> defaultChipPackages, BuyChipsPromotionForm form) {
        final Map<Platform, BigDecimal[]> defaultChipAmounts = ChipPackageHelper.getDefaultChipAmountsForAllPlatforms(defaultChipPackages);

        for (Map.Entry<Platform, BigDecimal[]> platformEntry : defaultChipAmounts.entrySet()) {
            for (BigDecimal defaultAmount : platformEntry.getValue()) {
                form.getPlatformToDefaultToOverriddenChipAmounts().get(platformEntry.getKey()).put(defaultAmount, defaultAmount);
            }
        }

    }
}
