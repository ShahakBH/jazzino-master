package com.yazino.bi.payment;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.platform.community.PaymentPreferences;

import java.io.Serializable;
import java.math.BigDecimal;

public class PromotionPaymentOption implements Serializable {
    private static final long serialVersionUID = -3013518709168585091L;

    private PaymentPreferences.PaymentMethod promotionPaymentMethod;
    private Long promoId;
    private BigDecimal promotionChipsPerPurchase;
    private String rolloverHeader;
    private String rolloverText;

    public PromotionPaymentOption() {
    }

    public PromotionPaymentOption(final PaymentPreferences.PaymentMethod promotionPaymentMethod,
                                  final Long promoId,
                                  final BigDecimal promotionChipsPerPurchase,
                                  final String rolloverHeader,
                                  final String rolloverText) {
        this.promotionPaymentMethod = promotionPaymentMethod;
        this.promoId = promoId;
        this.promotionChipsPerPurchase = promotionChipsPerPurchase;
        this.rolloverHeader = rolloverHeader;
        this.rolloverText = rolloverText;
    }

    public PaymentPreferences.PaymentMethod getPromotionPaymentMethod() {
        return promotionPaymentMethod;
    }

    public Long getPromoId() {
        return promoId;
    }

    public BigDecimal getPromotionChipsPerPurchase() {
        return promotionChipsPerPurchase;
    }

    public String getRolloverHeader() {
        return rolloverHeader;
    }

    public String getRolloverText() {
        return rolloverText;
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

        final PromotionPaymentOption rhs = (PromotionPaymentOption) obj;
        return new EqualsBuilder()
                .append(promoId, rhs.promoId)
                .append(promotionChipsPerPurchase, rhs.promotionChipsPerPurchase)
                .append(promotionPaymentMethod, rhs.promotionPaymentMethod)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 31)
                .append(promoId)
                .append(promotionChipsPerPurchase)
                .append(promotionPaymentMethod)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
