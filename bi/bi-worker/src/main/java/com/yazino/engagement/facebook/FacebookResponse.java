package com.yazino.engagement.facebook;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class FacebookResponse {
    private final FacebookAppToUserRequestStatus status;
    private final String requestId;

    public FacebookResponse(final FacebookAppToUserRequestStatus status, final String requestId) {
        this.status = status;
        this.requestId = requestId;
    }

    public FacebookAppToUserRequestStatus getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
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
        final FacebookResponse rhs = (FacebookResponse) obj;
        return new EqualsBuilder()
                .append(status, rhs.status)
                .append(requestId, rhs.requestId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(status)
                .append(requestId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(status)
                .append(requestId)
                .toString();
    }

}
