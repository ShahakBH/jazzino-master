package com.yazino.platform.audit.message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionKeyMessage implements AuditMessage {
    private static final long serialVersionUID = -684664139522910249L;

    @JsonProperty("key")
    private SessionKey sessionKey;

    public SessionKeyMessage() {
    }

    public SessionKeyMessage(final SessionKey sessionKey) {
        notNull(sessionKey, "sessionKey may not be null");

        this.sessionKey = sessionKey;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public AuditMessageType getMessageType() {
        return AuditMessageType.SESSION_KEY;
    }

    public SessionKey getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(final SessionKey sessionKey) {
        this.sessionKey = sessionKey;
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
        final SessionKeyMessage rhs = (SessionKeyMessage) obj;
        return new EqualsBuilder()
                .append(sessionKey, rhs.sessionKey)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(sessionKey)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(sessionKey)
                .toString();
    }

}
