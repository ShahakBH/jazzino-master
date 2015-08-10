package com.yazino.web.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Purchase {
    private PurchaseStatus status;
    private String errorMessage; // null if CREATED or SUCCESS

    private String purchaseId; // internal Id, for Yazino reference
    private String currencyCode;
    private BigDecimal price;
    private BigDecimal chips; // promo chips if promotion
    private String externalId;
    private String productId;

    public Purchase() {
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }


    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getChips() {
        return chips;
    }

    public void setChips(BigDecimal chips) {
        this.chips = chips;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(final String productId) {
        this.productId = productId;
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
        Purchase rhs = (Purchase) obj;
        return new EqualsBuilder()
                .append(this.status, rhs.status)
                .append(this.errorMessage, rhs.errorMessage)
                .append(this.purchaseId, rhs.purchaseId)
                .append(this.currencyCode, rhs.currencyCode)
                .append(this.price, rhs.price)
                .append(this.chips, rhs.chips)
                .append(this.externalId, rhs.externalId)
                .append(this.productId, rhs.productId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(status)
                .append(errorMessage)
                .append(purchaseId)
                .append(currencyCode)
                .append(price)
                .append(chips)
                .append(externalId)
                .append(productId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("status", status)
                .append("errorMessage", errorMessage)
                .append("purchaseId", purchaseId)
                .append("currencyCode", currencyCode)
                .append("price", price)
                .append("chips", chips)
                .append("externalId", externalId)
                .append("productId", productId)
                .toString();
    }
}
