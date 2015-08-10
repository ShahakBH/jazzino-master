package com.yazino.bi.operations.view;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.util.CollectionUtils;
import strata.server.lobby.api.promotion.BuyChipsPromotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;
import strata.server.lobby.api.promotion.PromotionType;
import strata.server.operations.promotion.model.ChipPackage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static strata.server.lobby.api.promotion.BuyChipsPromotion.*;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.MAX_REWARDS_KEY;


public class BuyChipsPromotionForm extends PromotionForm<BuyChipsPromotion> {

    private List<PaymentPreferences.PaymentMethod> paymentMethods = new ArrayList<PaymentPreferences.PaymentMethod>();

    private String inGameNotificationMsg;
    private String inGameNotificationHeader;

    private String rolloverHeader;
    private String rolloverText;

    private final Map<Platform, Map<BigDecimal, BigDecimal>> platformToDefaultToOverriddenChipAmounts = new HashMap<Platform, Map<BigDecimal, BigDecimal>>();

    public BuyChipsPromotionForm() {
        setPromotionType(PromotionType.BUY_CHIPS);
        setupOverrideMaps();
    }

    public BuyChipsPromotionForm(final BuyChipsPromotion promotion) {
        super(promotion);
        setupOverrideMaps();
        final PromotionConfiguration config = promotion.getConfiguration();
        if (config != null && config.hasConfigItems()) {
            final String paymentMethodsStr = config.getConfigurationValue(PAYMENT_METHODS_KEY);
            paymentMethods = new ArrayList<PaymentPreferences.PaymentMethod>();
            if (StringUtils.isNotBlank(paymentMethodsStr)) {
                for (String method : paymentMethodsStr.split(",")) {
                    paymentMethods.add(PaymentPreferences.PaymentMethod.valueOf(method));
                }
            }
            inGameNotificationMsg = config.getConfigurationValue(IN_GAME_NOTIFICATION_MSG_KEY);
            inGameNotificationHeader = config.getConfigurationValue(IN_GAME_NOTIFICATION_HEADER_KEY);

            rolloverHeader = config.getConfigurationValue(ROLLOVER_HEADER_KEY);
            rolloverText = config.getConfigurationValue(ROLLOVER_TEXT_KEY);
        }
    }

    private void setupOverrideMaps() {
        for (Platform platform : Platform.values()) {
            platformToDefaultToOverriddenChipAmounts.put(platform, new HashMap<BigDecimal, BigDecimal>());
        }
    }

    public Map<Platform, Map<BigDecimal, BigDecimal>> getPlatformToDefaultToOverriddenChipAmounts() {
        return platformToDefaultToOverriddenChipAmounts;
    }

    public List<PaymentPreferences.PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(final List<PaymentPreferences.PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public String getInGameNotificationMsg() {
        return inGameNotificationMsg;
    }

    public void setInGameNotificationMsg(final String inGameNotificationMsg) {
        this.inGameNotificationMsg = inGameNotificationMsg;
    }

    public String getInGameNotificationHeader() {
        return inGameNotificationHeader;
    }

    public void setInGameNotificationHeader(final String inGameNotificationHeader) {
        this.inGameNotificationHeader = inGameNotificationHeader;
    }

    public String getRolloverHeader() {
        return rolloverHeader;
    }

    public void setRolloverHeader(final String rolloverHeader) {
        this.rolloverHeader = rolloverHeader;
    }

    public String getRolloverText() {
        return rolloverText;
    }

    public void setRolloverText(final String rolloverText) {
        this.rolloverText = rolloverText;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public BuyChipsPromotion buildPromotion() {
        BuyChipsPromotion promotion = super.buildPromotion();
        promotion.addConfigurationItem(MAX_REWARDS_KEY, getMaximumRewards().toString());
        if (!CollectionUtils.isEmpty(paymentMethods)) {
            promotion.addConfigurationItem(PAYMENT_METHODS_KEY, toPaymentMethodList());
        }
        promotion.addConfigurationItem(IN_GAME_NOTIFICATION_MSG_KEY, inGameNotificationMsg);
        promotion.addConfigurationItem(IN_GAME_NOTIFICATION_HEADER_KEY, inGameNotificationHeader);

        promotion.addConfigurationItem(ROLLOVER_HEADER_KEY, rolloverHeader);
        promotion.addConfigurationItem(ROLLOVER_TEXT_KEY, rolloverText);

        return promotion;
    }

    // this weirdness occurs because we are handling a data persistence concern (store the config only where it is not the defalt) in the web layer...
    public void updateConfigurationWithOverriddenChipAmounts(BuyChipsPromotion promotion, Map<Platform, List<ChipPackage>> defaultPackages) {
        for (Map.Entry<Platform, List<ChipPackage>> entry : defaultPackages.entrySet()) {
            Platform platform = entry.getKey();
            if (!getPlatforms().contains(platform)) {
                continue;
            }
            List<ChipPackage> defaultChipAmounts = entry.getValue();
            Map<BigDecimal, BigDecimal> overriddenChipAmounts = platformToDefaultToOverriddenChipAmounts.get(platform);
            for (ChipPackage chipPackage : defaultChipAmounts) {
                BigDecimal overrideChipAmount = overriddenChipAmounts.get(chipPackage.getDefaultChips());
                if (overrideChipAmount != null) {
                    promotion.configureChipsForPlatformAndPackage(platform, chipPackage.getDefaultChips(), overrideChipAmount);
                }
            }
        }
    }

    private String toPaymentMethodList() {
        List<String> paymentMethodNames = new ArrayList<String>();
        for (PaymentPreferences.PaymentMethod paymentMethod : paymentMethods) {
            paymentMethodNames.add(paymentMethod.name());
        }
        return StringUtils.join(paymentMethodNames, ',');
    }

    public boolean isPaymentMethodEnabled(String name) {
        return paymentMethods.contains(PaymentPreferences.PaymentMethod.valueOf(name));
    }

}
