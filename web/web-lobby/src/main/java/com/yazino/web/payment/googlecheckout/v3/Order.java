package com.yazino.web.payment.googlecheckout.v3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {
    private String orderId;
    private String packageName;
    private String productId;
    private GooglePurchaseOrderState purchaseState;
    private String purchaseToken;
    private DateTime purchaseTime;
    private String developerPayload;

    public String getDeveloperPayload() {
        return developerPayload;
    }

    public void setDeveloperPayload(String developerPayload) {
        this.developerPayload = developerPayload;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public void setPurchaseToken(String purchaseToken) {
        this.purchaseToken = purchaseToken;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public GooglePurchaseOrderState getPurchaseState() {
        return purchaseState;
    }

    public void setPurchaseState(GooglePurchaseOrderState purchaseState) {
        this.purchaseState = purchaseState;
    }

    public DateTime getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(DateTime purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("orderId", orderId)
                .append("packageName", packageName)
                .append("productId", productId)
                .append("purchaseState", purchaseState)
                .append("purchaseToken", purchaseToken)
                .append("purchaseTime", purchaseTime)
                .append("developerPayload", developerPayload)
                .toString();
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
        Order rhs = (Order) obj;
        return new EqualsBuilder()
                .append(this.orderId, rhs.orderId)
                .append(this.packageName, rhs.packageName)
                .append(this.productId, rhs.productId)
                .append(this.purchaseState, rhs.purchaseState)
                .append(this.purchaseToken, rhs.purchaseToken)
                .append(this.purchaseTime, rhs.purchaseTime)
                .append(this.developerPayload, rhs.developerPayload)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(orderId)
                .append(packageName)
                .append(productId)
                .append(purchaseState)
                .append(purchaseToken)
                .append(purchaseTime)
                .append(developerPayload)
                .toHashCode();
    }


}
