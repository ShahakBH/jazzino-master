package com.yazino.bi.payment;

import com.google.common.collect.ComparisonChain;
import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import static java.lang.String.format;
import static org.springframework.util.Assert.notNull;

public class PaymentOption implements Serializable, Comparable {
    private static final long serialVersionUID = 4326357560084220282L;

    private String id;
    private int level;
    private BigDecimal numChipsPerPurchase;
    private BigDecimal amountRealMoneyPerPurchase;
    private String realMoneyCurrency;
    private String currencyLabel;
    private String currencyCode;
    private String title;
    private String description;
    private String upsellId;
    private String upsellTitle;
    private String upsellDescription;
    private BigDecimal upsellNumChipsPerPurchase;
    private BigDecimal upsellRealMoneyPerPurchase;
    private String baseCurrencyCode;
    private BigDecimal baseCurrencyPrice;
    private BigDecimal exchangeRate;

    private Map<PaymentPreferences.PaymentMethod, PromotionPaymentOption> promotions;

    public PaymentOption() {
    }

    public PaymentOption(final PaymentOption paymentOption) {
        notNull(paymentOption, "paymentOption may not be null");

        this.id = paymentOption.id;
        this.level = paymentOption.level;
        this.numChipsPerPurchase = paymentOption.numChipsPerPurchase;
        this.amountRealMoneyPerPurchase = paymentOption.amountRealMoneyPerPurchase;
        this.realMoneyCurrency = paymentOption.realMoneyCurrency;
        this.currencyLabel = paymentOption.currencyLabel;
        this.currencyCode = paymentOption.currencyCode;
        this.title = paymentOption.title;
        this.description = paymentOption.description;
        this.upsellId = paymentOption.upsellId;
        this.upsellTitle = paymentOption.upsellTitle;
        this.upsellDescription = paymentOption.upsellDescription;
        this.upsellNumChipsPerPurchase = paymentOption.upsellNumChipsPerPurchase;
        this.upsellRealMoneyPerPurchase = paymentOption.upsellRealMoneyPerPurchase;
        this.baseCurrencyPrice = paymentOption.baseCurrencyPrice;
        this.baseCurrencyCode = paymentOption.baseCurrencyCode;
        this.exchangeRate = paymentOption.exchangeRate;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(final int level) {
        this.level = level;
    }

    public String getUpsellId() {
        return upsellId;
    }

    public void setUpsellId(final String upsellId) {
        this.upsellId = upsellId;
    }

    public BigDecimal getAmountRealMoneyPerPurchase() {
        return amountRealMoneyPerPurchase;
    }

    public String getUpsellMessage() {
        return format("THAT'S %s chips per %s1",
                formatBd(numChipsPerPurchase.divide(amountRealMoneyPerPurchase, 0)),
                currencyLabel);
    }

    public String getBaseCurrencyCode() {
        return baseCurrencyCode;
    }

    public void setBaseCurrencyCode(final String baseCurrencyCode) {
        this.baseCurrencyCode = baseCurrencyCode;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(final BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getBaseCurrencyPrice() {
        return baseCurrencyPrice;
    }

    public void setBaseCurrencyPrice(final BigDecimal baseCurrencyPrice) {
        this.baseCurrencyPrice = baseCurrencyPrice;
    }

    private String formatBd(final BigDecimal value) {
        if (value == null) {
            return null;
        }

        final DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("en_US"));
        final DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        return formatter.format(value);
    }

    public String getRealMoneyCurrency() {
        return realMoneyCurrency;
    }

    public void setAmountRealMoneyPerPurchase(
            final BigDecimal amountRealMoneyPerPurchase) {
        this.amountRealMoneyPerPurchase = amountRealMoneyPerPurchase;
    }

    public void setNumChipsPerPurchase(final BigDecimal numChipsPerPurchase) {
        this.numChipsPerPurchase = numChipsPerPurchase;
    }

    public void setRealMoneyCurrency(final String realMoneyCurrency) {
        this.realMoneyCurrency = realMoneyCurrency;
    }

    public String getCurrencyLabel() {
        return currencyLabel;
    }

    public void setCurrencyLabel(final String currencyLabel) {
        this.currencyLabel = currencyLabel;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Map<PaymentPreferences.PaymentMethod, PromotionPaymentOption> getPromotions() {
        return promotions;
    }

    public void setPromotions(final Map<PaymentPreferences.PaymentMethod, PromotionPaymentOption> promotions) {
        this.promotions = promotions;
    }

    public void addPromotionPaymentOption(final PromotionPaymentOption promotionPaymentOption) {
        if (promotions == null) {
            promotions = new HashMap<PaymentPreferences.PaymentMethod, PromotionPaymentOption>(2);
        }
        promotions.put(promotionPaymentOption.getPromotionPaymentMethod(), promotionPaymentOption);
    }

    public boolean hasPromotion(final String paymentMethod) {
        if (promotions == null || StringUtils.isBlank(paymentMethod)) {
            return false;
        }
        return hasPromotion(PaymentPreferences.PaymentMethod.valueOf(paymentMethod));
    }

    public boolean hasPromotion(final PaymentPreferences.PaymentMethod paymentMethod) {
        return promotions != null && promotions.containsKey(paymentMethod);
    }

    /**
     * @return promotion for payment method or null if this payment option does not have a promotion
     */
    public PromotionPaymentOption getPromotion(final String paymentMethod) {
        if (promotions == null || StringUtils.isBlank(paymentMethod)) {
            return null;
        }
        return getPromotion(PaymentPreferences.PaymentMethod.valueOf(paymentMethod));
    }

    public PromotionPaymentOption getPromotion(final PaymentPreferences.PaymentMethod paymentMethod) {
        if (promotions != null) {
            return promotions.get(paymentMethod);
        }
        return null;
    }

    public BigDecimal getNumChipsPerPurchase() {
        return numChipsPerPurchase;
    }

    /*
     * returns number of chip for promotion if exists otherwise default number of chips
     */
    public BigDecimal getNumChipsPerPurchase(final String paymentMethod) {
        if (hasPromotion(paymentMethod)) {
            return getPromotion(paymentMethod).getPromotionChipsPerPurchase();
        }
        return numChipsPerPurchase;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return detokeniseDescription(description, numChipsPerPurchase, amountRealMoneyPerPurchase);
    }

    private String detokeniseDescription(final String tokenisedDesc,
                                         final BigDecimal chips,
                                         final BigDecimal price) {
        if (tokenisedDesc == null) {
            return null;
        }

        BigDecimal priceDelta = null;
        if (amountRealMoneyPerPurchase != null && upsellRealMoneyPerPurchase != null) {
            priceDelta = upsellRealMoneyPerPurchase.subtract(amountRealMoneyPerPurchase);
        }

        return tokenisedDesc
                .replaceAll("\\$CHIPS\\$", formatBd(chips))
                .replaceAll("\\$CURRENCY\\$", Matcher.quoteReplacement(currencyLabel))
                .replaceAll("\\$PRICE\\$", formatBd(price))
                .replaceAll("\\$PRICE_DELTA\\$", formatBd(priceDelta));
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getUpsellTitle() {
        return upsellTitle;
    }

    public void setUpsellTitle(final String upsellTitle) {
        this.upsellTitle = upsellTitle;
    }

    public String getUpsellDescription() {
        return detokeniseDescription(upsellDescription, upsellNumChipsPerPurchase, upsellRealMoneyPerPurchase);
    }

    public void setUpsellDescription(final String upsellDescription) {
        this.upsellDescription = upsellDescription;
    }

    public BigDecimal getUpsellNumChipsPerPurchase() {
        return upsellNumChipsPerPurchase;
    }

    public void setUpsellNumChipsPerPurchase(final BigDecimal upsellNumChipsPerPurchase) {
        this.upsellNumChipsPerPurchase = upsellNumChipsPerPurchase;
    }

    public BigDecimal getUpsellRealMoneyPerPurchase() {
        return upsellRealMoneyPerPurchase;
    }

    public void setUpsellRealMoneyPerPurchase(final BigDecimal upsellRealMoneyPerPurchase) {
        this.upsellRealMoneyPerPurchase = upsellRealMoneyPerPurchase;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final PaymentOption rhs = (PaymentOption) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(level, rhs.level)
                .append(upsellId, rhs.upsellId)
                .append(numChipsPerPurchase, rhs.numChipsPerPurchase)
                .append(amountRealMoneyPerPurchase, rhs.amountRealMoneyPerPurchase)
                .append(realMoneyCurrency, rhs.realMoneyCurrency)
                .append(currencyCode, rhs.currencyCode)
                .append(promotions, rhs.promotions)
                .append(title, rhs.title)
                .append(description, rhs.description)
                .append(upsellTitle, rhs.upsellTitle)
                .append(upsellDescription, rhs.upsellDescription)
                .append(upsellNumChipsPerPurchase, rhs.upsellNumChipsPerPurchase)
                .append(upsellRealMoneyPerPurchase, rhs.upsellRealMoneyPerPurchase)
                .append(baseCurrencyCode, rhs.baseCurrencyCode)
                .append(baseCurrencyPrice, rhs.baseCurrencyPrice)
                .append(exchangeRate, rhs.exchangeRate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(level)
                .append(upsellId)
                .append(numChipsPerPurchase)
                .append(amountRealMoneyPerPurchase)
                .append(realMoneyCurrency)
                .append(currencyLabel)
                .append(currencyCode)
                .append(promotions)
                .append(title)
                .append(description)
                .append(upsellTitle)
                .append(upsellDescription)
                .append(upsellNumChipsPerPurchase)
                .append(upsellRealMoneyPerPurchase)
                .append(baseCurrencyCode)
                .append(baseCurrencyPrice)
                .append(exchangeRate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int compareTo(final Object otherObject) {
        if (otherObject == null || !(otherObject instanceof PaymentOption)) {
            return 1;
        }

        final PaymentOption other = (PaymentOption) otherObject;
        return ComparisonChain.start()
                .compare(amountRealMoneyPerPurchase, other.amountRealMoneyPerPurchase)
                .result();
    }
}
