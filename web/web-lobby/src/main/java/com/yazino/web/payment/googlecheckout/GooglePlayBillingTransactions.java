package com.yazino.web.payment.googlecheckout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class GooglePlayBillingTransactions {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GooglePlayTransaction {
        private String notificationId;
        private String orderId;
        private String packageName;
        private String productId;
        private String purchaseState;
        private String purchaseToken;
        private DateTime purchaseTime;

        public String getPurchaseToken() {
            return purchaseToken;
        }

        public void setPurchaseToken(String purchaseToken) {
            this.purchaseToken = purchaseToken;
        }

        public String getNotificationId() {
            return notificationId;
        }

        public void setNotificationId(String notificationId) {
            this.notificationId = notificationId;
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

        public String getPurchaseState() {
            return purchaseState;
        }

        public void setPurchaseState(String purchaseState) {
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
            return "GooglePlayTransaction{"
                    + "notificationId='" + notificationId + '\''
                    + ", orderId='" + orderId + '\''
                    + ", packageName='" + packageName + '\''
                    + ", productId='" + productId + '\''
                    + ", purchaseState='" + purchaseState + '\''
                    + ", purchaseToken='" + purchaseToken + '\''
                    + ", purchaseTime=" + purchaseTime
                    + '}';
        }
    }

    @JsonProperty("orders")
    private List<GooglePlayTransaction> googlePlayTransactions;

    public List<GooglePlayTransaction> getGooglePlayTransactions() {
        if (googlePlayTransactions == null) {
            return Collections.emptyList();
        }
        return googlePlayTransactions;
    }

    public void setGooglePlayTransactions(List<GooglePlayTransaction> googlePlayTransaction) {
        this.googlePlayTransactions = googlePlayTransaction;
    }
}
