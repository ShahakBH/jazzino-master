package com.yazino.platform.player;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.platform.invitation.InvitationSource;

import static org.apache.commons.lang3.Validate.notBlank;

public class FacebookReferralRecipient implements ReferralRecipient {
    private static final long serialVersionUID = 7241530510766766625L;

    private final String externalId;

    public FacebookReferralRecipient(final String externalId) {
        notBlank(externalId, "externalId is null");
        this.externalId = externalId;
    }

    @Override
    public InvitationSource getInvitationSource() {
        return InvitationSource.FACEBOOK;
    }

    @Override
    public String getRecipientIdentifier() {
        return externalId;
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
        final FacebookReferralRecipient rhs = (FacebookReferralRecipient) obj;
        return new EqualsBuilder()
                .append(externalId, rhs.externalId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(externalId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(externalId)
                .toString();
    }
}
