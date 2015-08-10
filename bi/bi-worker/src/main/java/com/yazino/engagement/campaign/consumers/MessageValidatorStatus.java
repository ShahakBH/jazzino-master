package com.yazino.engagement.campaign.consumers;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MessageValidatorStatus {

    private final boolean status;
    private final String description;

    public MessageValidatorStatus(boolean status, String description) {
        this.status = status;
        this.description = description;
    }

    public boolean getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.status)
                .append(this.description).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MessageValidatorStatus other = (MessageValidatorStatus) obj;
        return new EqualsBuilder().append(this.status, other.status)
                .append(this.description, other.description).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("status", status)
                .append("description", description)
                .toString();
    }
}
