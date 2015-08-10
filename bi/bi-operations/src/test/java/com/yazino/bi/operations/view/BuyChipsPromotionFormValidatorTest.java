package com.yazino.bi.operations.view;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuyChipsPromotionFormValidatorTest extends PromotionFormValidatorTest {

    private static final Set<Float> VALID_IOS_PERCENTAGE_INCREASES = new HashSet<Float>(asList(0.0f, 10.0f, 25.0f, 50.0f, 100.0f, 150.0f, 200.0f));
    private static final Set<Float> VALID_ANDROID_PERCENTAGE_INCREASES = new HashSet<Float>(asList(0.0f,50.0f, 100.0f, 200.0f));
    private static final int IN_GAME_NOTIFICATION_HEADER_MAX_LENGTH = 30;
    private static final int IN_GAME_NOTIFICATION_MESSAGE_MAX_LENGTH = 75;
    public static final int DECIMAL_SCALE = 10;

    private PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer = mock(PaymentOptionsToChipPackageTransformer.class);

    private final static List<BigDecimal> DEFAULT_CHIP_AMOUNTS = asList(BigDecimal.valueOf(10000), BigDecimal.valueOf(21000));
    private HashMap<Platform, List<ChipPackage>> defaultChipPackages;

    @Override
    protected PromotionFormValidator initPromotionFormValidator() {
        return new BuyChipsPromotionFormValidator(paymentOptionsToChipPackageTransformer);
    }

    @Override
    protected PromotionForm createValidPromotionForm() {
        BuyChipsPromotionForm promotion = new BuyChipsPromotionForm();

        promotion.setName("aname");
        promotion.setPlatforms(Arrays.asList(Platform.WEB));
        promotion.setStartDate(new DateTime(2011, 7, 14, 0, 0, 0, 0));
        promotion.setStartHour(12);
        promotion.setStartMinute(30);
        promotion.setEndHour(10);
        promotion.setEndMinute(11);
        promotion.setEndDate(new DateTime(2011, 7, 21, 0, 0, 0, 0));

        promotion.setMaximumRewards(10);
        for (BigDecimal defaultChipAmount : DEFAULT_CHIP_AMOUNTS) {
            BigDecimal sampleMultiplier = BigDecimal.valueOf(2);
            promotion.getPlatformToDefaultToOverriddenChipAmounts().get(Platform.WEB).put(defaultChipAmount, defaultChipAmount.multiply(sampleMultiplier));
        }

        promotion.setPaymentMethods(asList(PaymentPreferences.PaymentMethod.CREDITCARD));
        return promotion;
    }

    @Before
    public void setupDefaultChipPackages() {
        List<ChipPackage> chipPackages = new ArrayList<ChipPackage>();

        for (BigDecimal defaultChipAmount : DEFAULT_CHIP_AMOUNTS) {

            PaymentOption paymentOption = new PaymentOption();
            paymentOption.setCurrencyLabel("$");
            paymentOption.setAmountRealMoneyPerPurchase(BigDecimal.ONE);
            paymentOption.setId("1");
            paymentOption.setNumChipsPerPurchase(defaultChipAmount);
            paymentOption.setRealMoneyCurrency("USD");
            Map<Currency, List<PaymentOption>> optionMap = new HashMap<Currency, List<PaymentOption>>();
            optionMap.put(Currency.USD, Arrays.asList(paymentOption));

            ChipPackage chipPackage = new ChipPackage();
            chipPackage.setDefaultChips(defaultChipAmount);
            chipPackages.add(chipPackage);
        }

        defaultChipPackages = new HashMap<Platform, List<ChipPackage>>();
        defaultChipPackages.put(Platform.WEB, chipPackages);
        defaultChipPackages.put(Platform.IOS, chipPackages);
        defaultChipPackages.put(Platform.ANDROID, chipPackages);
        defaultChipPackages.put(Platform.FACEBOOK_CANVAS, chipPackages);

        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(defaultChipPackages);
    }

    @Test
    public void supportsBuyChipsPromotionForm() {
        assertTrue(underTest.supports(BuyChipsPromotionForm.class));
    }

    @Test
    public void validateAddsErrorsWhenNoPaymentMethods() throws Exception {
        BuyChipsPromotionForm promotion = (BuyChipsPromotionForm) createValidPromotionForm();
        promotion.setPaymentMethods(new ArrayList<PaymentPreferences.PaymentMethod>());
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("paymentMethods"));
    }

    @Test
    public void inGameNotificationHeaderLimitedToMaxChars() {
        BuyChipsPromotionForm promotion = (BuyChipsPromotionForm) createValidPromotionForm();
        promotion.setInGameNotificationHeader(StringUtils.leftPad("", IN_GAME_NOTIFICATION_HEADER_MAX_LENGTH + 1, "x"));
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("inGameNotificationHeader"));
    }

    @Test
    public void inGameNotificationMessageLimitedToMaxChars() {
        BuyChipsPromotionForm promotion = (BuyChipsPromotionForm) createValidPromotionForm();
        promotion.setInGameNotificationMsg(StringUtils.leftPad("", IN_GAME_NOTIFICATION_MESSAGE_MAX_LENGTH + 1, "x"));
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasFieldErrors("inGameNotificationMsg"));
    }

    @Test
        public void shouldAllowAllValidAndroidPromotionPercentages() throws Exception {
            for (Float percentageIncrease : VALID_ANDROID_PERCENTAGE_INCREASES) {
                assertPromotionPercentageAllowed(percentageIncrease, true, Platform.ANDROID,
                        PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT );
            }
        }

    @Test
    public void shouldAllowAllValidIOSPromotionPercentages() throws Exception {
        for (Float percentageIncrease : VALID_IOS_PERCENTAGE_INCREASES) {
            assertPromotionPercentageAllowed(percentageIncrease, true, Platform.IOS,
                    PaymentPreferences.PaymentMethod.ITUNES);
        }
    }

    @Test
    public void shouldRejectInvalidIOSPromotionPercentages() throws Exception {
        float invalidPercentageIncrease = 13f;
        assertPromotionPercentageAllowed(invalidPercentageIncrease, false, Platform.IOS,
                            PaymentPreferences.PaymentMethod.ITUNES);
    }

    @Test
    public void shouldRejectInvalidAndroidPromotionPercentages() throws Exception {
        float invalidPercentageIncrease = 25f;
        assertPromotionPercentageAllowed(invalidPercentageIncrease, false, Platform.ANDROID,
                            PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT);
    }


    private void assertPromotionPercentageAllowed(Float percentageIncrease, boolean isValid, final Platform platform, final PaymentPreferences.PaymentMethod paymentMethod) {
            float percentage = 100 + percentageIncrease;
            BuyChipsPromotionForm promotion = (BuyChipsPromotionForm) createValidPromotionForm();
            promotion.setPlatforms(asList(platform));
            promotion.setPaymentMethods(asList(paymentMethod));
            boolean first = true;
            for (BigDecimal defaultChipAmount : DEFAULT_CHIP_AMOUNTS) {
                BigDecimal promotionChipAmount = asPercentageOf(percentage, defaultChipAmount);
                if (first) { // ensure at least one value is different to keep the promotion valid
                    promotionChipAmount = asPercentageOf(200, defaultChipAmount);
                }
                promotion.getPlatformToDefaultToOverriddenChipAmounts().get(platform).put(defaultChipAmount, promotionChipAmount);
                first = false;
            }
            Errors errors = new MapBindingResult(new HashMap(), "promotion");

            underTest.validate(promotion, errors);

            assertEquals(isValid, !errors.hasErrors());
        }

    private BigDecimal asPercentageOf(float percentage, BigDecimal defaultChipAmount) {
        return defaultChipAmount.multiply(BigDecimal.valueOf(percentage)).divide(BigDecimal.valueOf(100), DECIMAL_SCALE, RoundingMode.DOWN).setScale(0);
    }

    @Test
    public void testIosBuyChipPromotionMustBePredefinedPercentage_SmallExtraValue() {
        BuyChipsPromotionForm promotion = (BuyChipsPromotionForm) createValidPromotionForm();
        promotion.setPlatforms(asList(Platform.IOS));
        promotion.setPaymentMethods(asList(PaymentPreferences.PaymentMethod.ITUNES));

        for (BigDecimal defaultChipAmount : DEFAULT_CHIP_AMOUNTS) {
            promotion.getPlatformToDefaultToOverriddenChipAmounts().get(Platform.IOS).put(defaultChipAmount, defaultChipAmount.add(BigDecimal.ONE));
        }
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasErrors());
    }

    @Test
    public void shouldRejectPromotionWhereAllValuesAreDefault() {
        BuyChipsPromotionForm promotion = (BuyChipsPromotionForm) createValidPromotionForm();
        promotion.setPlatforms(asList(Platform.IOS));
        promotion.setPaymentMethods(asList(PaymentPreferences.PaymentMethod.ITUNES));

        for (BigDecimal defaultChipAmount : DEFAULT_CHIP_AMOUNTS) {
            promotion.getPlatformToDefaultToOverriddenChipAmounts().get(Platform.IOS).put(defaultChipAmount, defaultChipAmount);
        }
        Errors errors = new MapBindingResult(new HashMap(), "promotion");

        underTest.validate(promotion, errors);

        assertTrue(errors.hasErrors());
    }
}
