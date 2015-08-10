package com.yazino.web.domain;

import com.yazino.platform.player.PlayerProfile;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class EmailAddress implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    private String emailAddress;

    public EmailAddress() {
    }

    public EmailAddress(final PlayerProfile userProfile) {
        this(userProfile.getEmailAddress());
    }

    public EmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String setEmailAddress) {
        this.emailAddress = setEmailAddress;
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
        final EmailAddress rhs = (EmailAddress) obj;
        return new EqualsBuilder()
                .append(emailAddress, rhs.emailAddress)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(emailAddress)
                .toHashCode();
    }
}
