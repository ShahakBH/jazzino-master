package com.yazino.web.domain.email;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

public class InviteFriendsEmailDetails {

    private String callToActionUrl;
    private String customisedMessage;

    public InviteFriendsEmailDetails(final String message,
                                     final String referralUrl,
                                     final BigDecimal playerId) {
        customisedMessage = message;
        callToActionUrl = String.format(referralUrl, playerId);
    }

    public String getCallToActionUrl() {
        return callToActionUrl;
    }

    public String getCustomisedMessage() {
        return customisedMessage;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final InviteFriendsEmailDetails rhs = (InviteFriendsEmailDetails) obj;
        return new EqualsBuilder()
                .append(customisedMessage, rhs.customisedMessage)
                .append(callToActionUrl, rhs.callToActionUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(customisedMessage)
                .append(callToActionUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
