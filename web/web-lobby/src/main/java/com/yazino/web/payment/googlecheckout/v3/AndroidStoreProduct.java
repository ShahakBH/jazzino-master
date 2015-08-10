package com.yazino.web.payment.googlecheckout.v3;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class AndroidStoreProduct {
    private String productId;
    private BigDecimal promoChips;  // null if no promotion
    private BigDecimal defaultChips;

    public AndroidStoreProduct() {
    }

    public AndroidStoreProduct(String productId, BigDecimal defaultChips) {
        this.productId = productId;
        this.defaultChips = defaultChips;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getPromoChips() {
        return promoChips;
    }

    public void setPromoChips(BigDecimal promoChips) {
        this.promoChips = promoChips;
    }

    public BigDecimal getDefaultChips() {
        return defaultChips;
    }

    public void setDefaultChips(BigDecimal defaultChips) {
        this.defaultChips = defaultChips;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        AndroidStoreProduct rhs = (AndroidStoreProduct) obj;
        return new EqualsBuilder()
                .append(this.productId, rhs.productId)
                .append(this.promoChips, rhs.promoChips)
                .append(this.defaultChips, rhs.defaultChips)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(productId)
                .append(promoChips)
                .append(defaultChips)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "AndroidStoreProduct{"
                + "productId='" + productId + '\''
                + ", promoChips=" + promoChips
                + ", defaultChips=" + defaultChips + '}';
    }
}
