package com.yazino.engagement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookAppToUserMessage implements FacebookMessage {
    private static final long serialVersionUID = 2L;

    private Integer appRequestId;

    private Integer targetId;

    private FacebookMessageType messageType;

    public FacebookAppToUserMessage() {
    }

    public FacebookAppToUserMessage(FacebookMessageType messageType, final Integer appRequestId, final Integer targetId) {
        this.messageType = messageType;
        this.targetId = targetId;
        this.appRequestId = appRequestId;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public FacebookMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(FacebookMessageType messageType) {
        this.messageType = messageType;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public Integer getAppRequestId() {
        return appRequestId;
    }

    public void setTargetId(final Integer targetId) {
        this.targetId = targetId;
    }

    public void setAppRequestId(final Integer appRequestId) {
        this.appRequestId = appRequestId;
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

        final FacebookAppToUserMessage rhs = (FacebookAppToUserMessage) obj;
        return new EqualsBuilder().append(targetId, rhs.targetId)
                .append(appRequestId, rhs.appRequestId)
                .append(messageType, rhs.messageType)
                .isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(targetId)
                .append(appRequestId)
                .append(messageType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "FacebookAppToUserMessage{"
                + "messageType=" + messageType
                + ", appRequestId=" + appRequestId
                + ", targetId='" + targetId + '\''
                + '}';
    }
}
