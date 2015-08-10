package com.yazino.yaps;

import com.yazino.mobile.yaps.message.PushMessage;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A message with a target recipient.
 */
public class TargetedMessage {

    private final PushMessage message;
    private final String deviceToken;

    public TargetedMessage(final String deviceToken,
                           final PushMessage message) {
        this.deviceToken = deviceToken;
        this.message = message;
    }

    public PushMessage getMessage() {
        return message;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("message", message).
                append("deviceToken", deviceToken).
                toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.message)
                .append(this.deviceToken)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TargetedMessage other = (TargetedMessage) obj;
        return new EqualsBuilder()
                .append(this.message, other.message)
                .append(this.deviceToken, other.deviceToken)
                .isEquals();
    }
}
