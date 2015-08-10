package com.yazino.platform.email.message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static org.apache.commons.lang3.Validate.notBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailVerificationMessage implements EmailMessage {
    private static final long serialVersionUID = 3112641501432495003L;

    private static final int VERSION = 1;

    private String emailAddress;

    public EmailVerificationMessage() {
    }

    public EmailVerificationMessage(final String emailAddress) {
        notBlank(emailAddress, "emailAddress may not be null/blank");

        this.emailAddress = emailAddress;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public int getVersion() {
        return VERSION;
    }

    @Override
    public Object getMessageType() {
        return "verification";
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
        final EmailVerificationMessage rhs = (EmailVerificationMessage) obj;
        return new EqualsBuilder()
                .append(emailAddress, rhs.emailAddress)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(emailAddress)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(emailAddress)
                .toString();
    }
}

