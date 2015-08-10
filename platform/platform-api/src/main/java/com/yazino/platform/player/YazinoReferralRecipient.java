package com.yazino.platform.player;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.platform.invitation.InvitationSource;

import static org.apache.commons.lang3.Validate.notNull;

public class YazinoReferralRecipient implements ReferralRecipient {
    private static final long serialVersionUID = 7241530510766766625L;

    private final String emailAddress;

    public YazinoReferralRecipient(final String emailAddress) {
        notNull(emailAddress, "emailAddress is null");
        this.emailAddress = emailAddress;
    }

    @Override
    public InvitationSource getInvitationSource() {
        return InvitationSource.EMAIL;
    }

    @Override
    public String getRecipientIdentifier() {
        return emailAddress;
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
        final YazinoReferralRecipient rhs = (YazinoReferralRecipient) obj;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(emailAddress)
                .toString();
    }
}
