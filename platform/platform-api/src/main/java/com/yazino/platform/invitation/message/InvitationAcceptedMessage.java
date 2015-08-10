package com.yazino.platform.invitation.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yazino.platform.invitation.InvitationSource;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationAcceptedMessage implements InvitationMessage {
    private static final long serialVersionUID = -66723273810254718L;

    private static final int VERSION = 1;

    private String recipientIdentifier;
    private InvitationSource source;
    private DateTime registrationTime;
    private BigDecimal recipientPlayerId;

    public InvitationAcceptedMessage() {
    }

    public InvitationAcceptedMessage(final String recipientIdentifier,
                                     final InvitationSource source,
                                     final DateTime registrationTime,
                                     final BigDecimal recipientPlayerId) {
        notBlank(recipientIdentifier, "recipientIdentifier may not be null or blank");
        notNull(source, "source may not be null");
        notNull(registrationTime, "registrationTime may not be null");
        notNull(recipientPlayerId, "recipientPlayerIdentifier may not be null");

        this.recipientIdentifier = recipientIdentifier;
        this.source = source;
        this.registrationTime = registrationTime;
        this.recipientPlayerId = recipientPlayerId;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    @JsonIgnore
    public InvitationMessageType getMessageType() {
        return InvitationMessageType.INVITATION_ACCEPTED;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public void setRecipientIdentifier(final String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    public BigDecimal getRecipientPlayerId() {
        return recipientPlayerId;
    }

    public void setRecipientPlayerId(final BigDecimal recipientPlayerId) {
        this.recipientPlayerId = recipientPlayerId;
    }

    public InvitationSource getSource() {
        return source;
    }

    public void setSource(final InvitationSource source) {
        this.source = source;
    }

    public DateTime getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(final DateTime registrationTime) {
        this.registrationTime = registrationTime;
    }

    public boolean isValid() {
        return recipientIdentifier != null
                && recipientPlayerId != null
                && source != null
                && registrationTime != null;
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
        final InvitationAcceptedMessage rhs = (InvitationAcceptedMessage) obj;
        return new EqualsBuilder()
                .append(recipientIdentifier, rhs.recipientIdentifier)
                .append(recipientPlayerId, rhs.recipientPlayerId)
                .append(source, rhs.source)
                .append(registrationTime, rhs.registrationTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 19)
                .append(recipientIdentifier)
                .append(recipientPlayerId)
                .append(source)
                .append(registrationTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(recipientIdentifier)
                .append(recipientPlayerId)
                .append(source)
                .append(registrationTime)
                .toString();
    }
}
