package com.yazino.client.log;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientLogEvent implements Message<String> {

    public static final int VERSION = 1;
    private String messageType;
    private DateTime timestamp;
    private String payload;

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public String getMessageType() {
        return messageType;
    }

    public ClientLogEvent() {
    }

    public ClientLogEvent(DateTime timestamp, String payload, ClientLogEventMessageType messageType) {
        this.timestamp = timestamp;
        this.payload = payload;
        this.messageType = messageType.name();
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setMessageType(final String messageType) {
        this.messageType = messageType;
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
        ClientLogEvent rhs = (ClientLogEvent) obj;
        return new EqualsBuilder()
                .append(this.messageType, rhs.messageType)
                .append(this.timestamp, rhs.timestamp)
                .append(this.payload, rhs.payload)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(messageType)
                .append(timestamp)
                .append(payload)
                .toHashCode();
    }
}
