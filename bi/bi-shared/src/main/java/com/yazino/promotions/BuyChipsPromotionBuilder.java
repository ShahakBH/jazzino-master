package com.yazino.promotions;

import com.google.common.base.Joiner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import strata.server.lobby.api.promotion.BuyChipsPromotion;
import strata.server.lobby.api.promotion.ControlGroupFunctionType;
import strata.server.lobby.api.promotion.PromotionConfiguration;

import java.math.BigDecimal;
import java.util.List;

import static strata.server.lobby.api.promotion.BuyChipsPromotion.*;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.MAX_REWARDS_KEY;

public class BuyChipsPromotionBuilder implements PromotionBuilder<BuyChipsPromotion> {


    private BuyChipsPromotion promo;


    public BuyChipsPromotionBuilder() {
        promo = new BuyChipsPromotion();
    }

    public BuyChipsPromotion build() {
        return (BuyChipsPromotion) promo;
    }

    public BuyChipsPromotionBuilder withPlatforms(final List<Platform> platforms) {
        promo.setPlatforms(platforms);
        return this;
    }

    public BuyChipsPromotionBuilder withStartDate(final DateTime startDAte) {
        promo.setStartDate(startDAte);
        return this;
    }

    public BuyChipsPromotionBuilder withEndDate(final DateTime endDAte) {
        promo.setEndDate(endDAte);
        return this;
    }

    public BuyChipsPromotionBuilder withId(final Long id) {
        promo.setId(id);
        return this;
    }

    public BuyChipsPromotionBuilder withAllPlayers(boolean allPlayers) {
        promo.setAllPlayers(allPlayers);
        return this;
    }

    public BuyChipsPromotionBuilder withConfiguration(final PromotionConfiguration config) {
        promo.setConfiguration(config);
        return this;
    }

    public BuyChipsPromotionBuilder withName(final String name) {
        promo.setName(name);
        return this;
    }

    public BuyChipsPromotionBuilder withMaxRewards(Integer maxRewards) {
        if (maxRewards == null) {
            promo.getConfiguration().getConfiguration().remove(MAX_REWARDS_KEY);
        } else {
            promo.getConfiguration().addConfigurationItem(MAX_REWARDS_KEY, maxRewards.toString());
        }
        return this;
    }

    public BuyChipsPromotionBuilder withPriority(Integer priority) {
        promo.setPriority(priority);
        return this;
    }

    public BuyChipsPromotionBuilder withChips(String defaultPackage, String promoChips, Platform client) {
        if (StringUtils.isBlank(promoChips)) {
            promo.getConfiguration().removeChipAmountOverrideForPlatformAndPackage(client, new BigDecimal(defaultPackage));
        } else {
            promo.getConfiguration().overrideChipAmountForPlatformAndPackage(client, new BigDecimal(defaultPackage), new BigDecimal(promoChips));
        }
        return this;
    }

    public BuyChipsPromotionBuilder withPaymentMethod(PaymentPreferences.PaymentMethod paymentMethod) {
        promo.addConfigurationItem(PAYMENT_METHODS_KEY, paymentMethod.name());
        return this;
    }

    public BuyChipsPromotionBuilder withPaymentMethods(List<PaymentPreferences.PaymentMethod> paymentMethods) {

        String paymentMethodsAsCommaSeparatedString = Joiner.on(",").join(paymentMethods);
        promo.addConfigurationItem(PAYMENT_METHODS_KEY, paymentMethodsAsCommaSeparatedString);
        return this;
    }

    public BuyChipsPromotionBuilder withInGameHeader(String header) {
        promo.addConfigurationItem(IN_GAME_NOTIFICATION_HEADER_KEY, header);
        return this;
    }

    public BuyChipsPromotionBuilder withInGameMessage(String msg) {
        promo.addConfigurationItem(IN_GAME_NOTIFICATION_MSG_KEY, msg);
        return this;
    }

    public BuyChipsPromotionBuilder withRollOverHeaderValue(String value) {
        promo.addConfigurationItem(ROLLOVER_HEADER_KEY, value);
        return this;
    }

    public BuyChipsPromotionBuilder withRollOverTextValue(String value) {
        promo.addConfigurationItem(ROLLOVER_TEXT_KEY, value);
        return this;
    }

    public BuyChipsPromotionBuilder withControlGroupFunction(final ControlGroupFunctionType controlGroupFunction) {
        promo.setControlGroupFunction(controlGroupFunction);
        return this;
    }

    public BuyChipsPromotionBuilder withControlGroupPercentage(Integer controlGroupPercentage) {
        promo.setControlGroupPercentage(controlGroupPercentage);
        return this;
    }

    public BuyChipsPromotionBuilder withSeed(int seed) {
        promo.setSeed(seed);
        return this;
    }

    public BuyChipsPromotionBuilder withDefaultChipsForPlatformAndPackage(BigDecimal defaultChipAmount, Platform web) {
        promo.getConfiguration().removeChipAmountOverrideForPlatformAndPackage(web, defaultChipAmount);
        return this;
    }
}
