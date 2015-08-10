package com.yazino.platform.invitation;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

@SuppressWarnings("unused")
public class Invitation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BigDecimal issuingPlayerId;
    private final String recipientIdentifier;
    private final InvitationSource source;
    private final InvitationStatus status;
    private final DateTime lastUpdated;
    private final DateTime created;
    private final BigDecimal chipsEarned;

    public Invitation(final BigDecimal issuingPlayerId,
                      final String recipientIdentifier,
                      final InvitationSource source,
                      final InvitationStatus status,
                      final DateTime created,
                      final DateTime lastUpdated,
                      final BigDecimal chipsEarned) {
        this.issuingPlayerId = issuingPlayerId;
        this.recipientIdentifier = recipientIdentifier;
        this.source = source;
        this.status = status;
        this.created = created;
        this.lastUpdated = lastUpdated;
        this.chipsEarned = chipsEarned;
    }

    public BigDecimal getIssuingPlayerId() {
        return issuingPlayerId;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public InvitationSource getSource() {
        return source;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public DateTime getLastUpdated() {
        return lastUpdated;
    }

    public DateTime getCreated() {
        return created;
    }

    public BigDecimal getChipsEarned() {
        return chipsEarned;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Invitation)) {
            return false;
        }
        final Invitation castOther = (Invitation) other;
        return new EqualsBuilder()
                .append(issuingPlayerId, castOther.issuingPlayerId)
                .append(recipientIdentifier, castOther.recipientIdentifier)
                .append(source, castOther.source)
                .append(status, castOther.status)
                .append(created, castOther.created)
                .append(lastUpdated, castOther.lastUpdated)
                .append(chipsEarned, castOther.chipsEarned)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(issuingPlayerId)
                .append(recipientIdentifier)
                .append(source)
                .append(status)
                .append(created)
                .append(lastUpdated)
                .append(chipsEarned)
                .toHashCode();

    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
