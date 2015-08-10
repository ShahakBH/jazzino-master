package com.yazino.bi.operations.view;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod;
import static com.yazino.platform.payment.PaymentMethodsFactory.getPaymentMethodsForPlatform;
import static java.util.Arrays.asList;

@Service("buyChipsPromotionFormValidator")
public class BuyChipsPromotionFormValidator extends PromotionFormValidator {

    public static final int DECIMAL_SCALE = 10;

    private static final Set<Float> VALID_IOS_PERCENTAGE_INCREASES = new HashSet<>(asList(0.0f, 10.0f, 25.0f, 50.0f, 100.0f, 150.0f, 200.0f));

    private static final Set<Float> VALID_ANDROID_PERCENTAGE_INCREASES = new HashSet<>(asList(0.0f, 50.0f, 100.0f, 200.0f));

    private static final int IN_GAME_NOTIFICATION_HEADER_MAX_LENGTH = 30;
    private static final int IN_GAME_NOTIFICATION_MESSAGE_MAX_LENGTH = 75;

    private final PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer;

    @Autowired
    public BuyChipsPromotionFormValidator(PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer) {
        this.paymentOptionsToChipPackageTransformer = paymentOptionsToChipPackageTransformer;
    }

    @Override
    public boolean supports(final Class<?> aClass) {
        return BuyChipsPromotionForm.class.equals(aClass);
    }

    @Override
    public void validate(Object object, Errors errors) {
        final BuyChipsPromotionForm form = (BuyChipsPromotionForm) object;
        super.validate(form, errors);

        validatePaymentMethods(errors, form);
        validatePackageAmounts(errors, form);

        validateInGameHeader(errors, form);
        validateInGameMessage(errors, form);
    }

    private void validatePackageAmounts(Errors errors, BuyChipsPromotionForm form) {
        for (Platform platform : form.getPlatforms()) {
            if (form.getPlatformToDefaultToOverriddenChipAmounts().containsKey(platform)) {
                validatePaymentMethods(errors, platform, form.getPaymentMethods());
                validateChipPackagesForPlatform(errors, platform, form.getPlatformToDefaultToOverriddenChipAmounts().get(platform));
                validateMobilePackageAmounts(errors, form, platform);
            }
        }
    }

    private void validateMobilePackageAmounts(final Errors errors, final BuyChipsPromotionForm form, final Platform platform) {
        if (platform == Platform.IOS) {
            validatePercentageIncreases(errors,
                    form,
                    platform,
                    VALID_IOS_PERCENTAGE_INCREASES);
        } else if (platform == Platform.ANDROID) {
            validatePercentageIncreases(errors,
                    form,
                    platform,
                    VALID_ANDROID_PERCENTAGE_INCREASES);
        }
    }

    private void validatePaymentMethods(Errors errors, Platform platform, List<PaymentPreferences.PaymentMethod> paymentMethods) {
        PaymentMethod[] availablePaymentMethods = getPaymentMethodsForPlatform(platform);
        validatePaymentMethodsForPlatform(platform, errors, paymentMethods, availablePaymentMethods);
    }

    private void validatePaymentMethodsForPlatform(Platform platform,
                                                   Errors errors,
                                                   List<PaymentPreferences.PaymentMethod> selected,
                                                   PaymentMethod... available) {
        for (PaymentMethod paymentMethod : available) {
            if (selected.contains(paymentMethod)) {
                return;
            }
        }
        errors.rejectValue("paymentMethods", "payment.method.required", "Choose one or more payment methods for " + platform.name());
    }


    private void validateChipPackagesForPlatform(Errors errors, Platform platform, Map<BigDecimal, BigDecimal> values) {
        String fieldName = platform.name();

        List<ChipPackage> chipPackages = paymentOptionsToChipPackageTransformer.getDefaultPackages().get(platform);
        boolean variesAtLeastOneAmount = false;
        for (ChipPackage chipPackage : chipPackages) {
            BigDecimal chipAmount = values.get(chipPackage.getDefaultChips());
            if (chipAmount == null) {
                errors.reject(fieldName, "please specify all chip values (" + fieldName + ")");
            } else if (!chipAmount.equals(chipPackage.getDefaultChips())) {
                variesAtLeastOneAmount = true;
            }
        }
        if (!variesAtLeastOneAmount) {
            errors.reject(fieldName, "please change at least one of the chip values (" + fieldName + ")");
        }
    }


    private void validatePaymentMethods(final Errors errors, final BuyChipsPromotionForm buyChipsPromotionForm) {
        if (CollectionUtils.isEmpty(buyChipsPromotionForm.getPaymentMethods())) {
            errors.rejectValue("paymentMethods", "payment.method.required", "Choose one or more payment methods");
        }
    }

    private void validatePercentageIncreases(final Errors errors,
                                             final BuyChipsPromotionForm buyChipsPromotionForm,
                                             final Platform platform,
                                             final Set<Float> validPercentageIncreases) {


        final List<ChipPackage> chipAmounts = paymentOptionsToChipPackageTransformer.getDefaultPackages().get(platform);
        final BigDecimal[] newChipAmounts = new BigDecimal[chipAmounts.size()];
        int newChipAmountIndex = 0;
        for (ChipPackage chipAmount : chipAmounts) {
            BigDecimal effectiveAmount = chipAmount.getDefaultChips();
            BigDecimal override = buyChipsPromotionForm
                    .getPlatformToDefaultToOverriddenChipAmounts()
                    .get(platform)
                    .get(effectiveAmount);
            if (override != null) {
                effectiveAmount = override;
            }
            newChipAmounts[newChipAmountIndex++] = effectiveAmount;
        }
        for (int i = 0; i < newChipAmounts.length; i++) {
            final BigDecimal chipAmount = newChipAmounts[i];
            if (chipAmount != null) {
                final BigDecimal defaultChipAmount = chipAmounts.get(i).getDefaultChips();

                addInvalidPercentageIncreaseToErrors(errors,
                        chipAmount,
                        defaultChipAmount,
                        validPercentageIncreases,
                        platform);
            }
        }
    }

    private void addInvalidPercentageIncreaseToErrors(final Errors errors,
                                                      final BigDecimal chipAmount,
                                                      final BigDecimal defaultChipAmount,
                                                      final Set<Float> validPercentageIncreases,
                                                      final Platform platform) {

        if (!isPercentageIncreaseValid(defaultChipAmount, chipAmount, validPercentageIncreases)) {
            if (platform == Platform.IOS) {
                rejectPercentageIncreaseForIos(errors, defaultChipAmount);
            } else if (platform == Platform.ANDROID) {
                rejectPercentageIncreaseForAndroid(errors, defaultChipAmount);
            }
        }
    }

    final boolean isPercentageIncreaseValid(final BigDecimal defaultChipAmount,
                                            final BigDecimal newChipAmount,
                                            final Set<Float> validPercentageIncreases) {

        final BigDecimal extraChips = newChipAmount.subtract(defaultChipAmount);
        final float percentageIncrease = extraChips.divide(defaultChipAmount,
                DECIMAL_SCALE, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).floatValue();
        return validPercentageIncreases.contains(percentageIncrease);
    }

    private void validateInGameHeader(final Errors errors, final BuyChipsPromotionForm buyChipsPromotionForm) {
        final String inGameNotificationHeader = buyChipsPromotionForm.getInGameNotificationHeader();
        if (StringUtils.isNotEmpty(inGameNotificationHeader)
                && inGameNotificationHeader.length() > IN_GAME_NOTIFICATION_HEADER_MAX_LENGTH) {
            errors.rejectValue("inGameNotificationHeader", "inGameNotificationHeader.length",
                    "In Game Notification Header: maximum length is " + IN_GAME_NOTIFICATION_HEADER_MAX_LENGTH);
        }
    }

    private void validateInGameMessage(final Errors errors, final BuyChipsPromotionForm buyChipsPromotionForm) {
        final String inGameNotificationMsg = buyChipsPromotionForm.getInGameNotificationMsg();
        if (StringUtils.isNotEmpty(inGameNotificationMsg)
                && inGameNotificationMsg.length() > IN_GAME_NOTIFICATION_MESSAGE_MAX_LENGTH) {
            errors.rejectValue("inGameNotificationMsg", "inGameNotificationMsg.length",
                    "In Game Notification Message: maximum length is " + IN_GAME_NOTIFICATION_MESSAGE_MAX_LENGTH);
        }
    }

    private void rejectPercentageIncreaseForIos(final Errors errors,
                                                final BigDecimal defaultChipAmount) {

        errors.rejectValue("platformToDefaultToOverriddenChipAmounts[IOS][" + defaultChipAmount + "]",
                "chip.override.invalid.percentage",
                "IOS Promo chips can only be set at 100%, 110%, 125%, 150%, 200%, 250%, 300% of the default "
                        + defaultChipAmount + ". i.e. "
                        + defaultChipAmount.multiply(new BigDecimal("1.1")).toBigInteger() + " ,"
                        + defaultChipAmount.multiply(new BigDecimal("1.25")).toBigInteger() + " ,"
                        + defaultChipAmount.multiply(new BigDecimal("1.5")).toBigInteger() + " ,"
                        + defaultChipAmount.multiply(new BigDecimal("2.0")).toBigInteger() + " ,"
                        + defaultChipAmount.multiply(new BigDecimal("2.5")).toBigInteger() + " or "
                        + defaultChipAmount.multiply(new BigDecimal("3.0")).toBigInteger()
        );
    }

    private void rejectPercentageIncreaseForAndroid(final Errors errors,
                                                    final BigDecimal defaultChipAmount) {

        errors.rejectValue("platformToDefaultToOverriddenChipAmounts[ANDROID][" + defaultChipAmount + "]",
                "chip.override.invalid.percentage",
                "Android Promo chips can only be set at 100%, 150%, 200% of the default "
                        + defaultChipAmount + ". i.e. "
                        + defaultChipAmount.multiply(new BigDecimal("1.5")).toBigInteger() + " ,"
                        + defaultChipAmount.multiply(new BigDecimal("2.0")).toBigInteger() + " ,"
        );
    }
}
