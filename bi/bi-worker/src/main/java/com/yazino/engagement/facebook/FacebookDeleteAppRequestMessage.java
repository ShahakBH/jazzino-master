package com.yazino.engagement.facebook;

import com.yazino.engagement.campaign.AppRequestExternalReference;
import com.yazino.engagement.FacebookMessage;
import com.yazino.engagement.FacebookMessageType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookDeleteAppRequestMessage implements FacebookMessage {
    private static final long serialVersionUID = 1L;

    private AppRequestExternalReference appRequestExternalReference;

    public FacebookDeleteAppRequestMessage() {
    }

    public FacebookDeleteAppRequestMessage(final AppRequestExternalReference appRequestExternalReference) {
        this.appRequestExternalReference = appRequestExternalReference;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public FacebookMessageType getMessageType() {
        return FacebookMessageType.DELETE_REQUEST;
    }

    public AppRequestExternalReference getAppRequestExternalReference() {
        return appRequestExternalReference;
    }

    public void setAppRequestExternalReference(final AppRequestExternalReference appRequestExternalReference) {
        this.appRequestExternalReference = appRequestExternalReference;
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
        final FacebookDeleteAppRequestMessage rhs = (FacebookDeleteAppRequestMessage) obj;
        return new EqualsBuilder()
                .append(appRequestExternalReference, rhs.appRequestExternalReference)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(appRequestExternalReference)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(appRequestExternalReference)
                .toString();
    }
}
