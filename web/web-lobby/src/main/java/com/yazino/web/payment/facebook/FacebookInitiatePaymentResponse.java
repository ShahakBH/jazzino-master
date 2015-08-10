package com.yazino.web.payment.facebook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class FacebookInitiatePaymentResponse {
    private final String requestId;
    private final String productUrl;

    public FacebookInitiatePaymentResponse(final String requestId, final String productUrl) {
        this.requestId = requestId;
        this.productUrl = productUrl;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getProductUrl() {
        return productUrl;
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
        FacebookInitiatePaymentResponse rhs = (FacebookInitiatePaymentResponse) obj;
        return new EqualsBuilder()
                .append(this.requestId, rhs.requestId)
                .append(this.productUrl, rhs.productUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(requestId)
                .append(productUrl)
                .toHashCode();
    }
}
