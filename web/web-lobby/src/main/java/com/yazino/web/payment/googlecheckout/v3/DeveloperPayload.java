package com.yazino.web.payment.googlecheckout.v3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeveloperPayload {
    private String purchaseId;

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("purchaseId", purchaseId)
                .toString();
    }
}
