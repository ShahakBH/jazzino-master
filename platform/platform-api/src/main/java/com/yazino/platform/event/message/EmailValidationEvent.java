package com.yazino.platform.event.message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailValidationEvent implements PlatformEvent {

    @JsonProperty("addr")
    private String emailAddress;

    @JsonProperty("st")
    private String status;

    private EmailValidationEvent() {
    }

    public EmailValidationEvent(final String emailAddress, final String status) {
        this.emailAddress = emailAddress;
        this.status = status;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.EMAIL_VALIDATION;
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
        EmailValidationEvent rhs = (EmailValidationEvent) obj;
        return new EqualsBuilder()
                .append(this.emailAddress, rhs.emailAddress)
                .append(this.status, rhs.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(emailAddress)
                .append(status)
                .toHashCode();
    }
}
