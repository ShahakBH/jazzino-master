package com.yazino.platform.invitation.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yazino.platform.invitation.InvitationSource;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationNotAcceptedMessage implements InvitationMessage {
    private static final long serialVersionUID = -8402188815342383159L;

    private static final int VERSION = 1;

    private String recipientIdentifier;
    private InvitationSource source;
    private DateTime updatedTime;

    public InvitationNotAcceptedMessage() {
    }

    public InvitationNotAcceptedMessage(final String recipientIdentifier,
                                        final InvitationSource source,
                                        final DateTime updatedTime) {
        notBlank(recipientIdentifier, "recipientIdentifier may not be null or blank");
        notNull(source, "source may not be null");
        notNull(updatedTime, "updatedTime may not be null");

        this.recipientIdentifier = recipientIdentifier;
        this.source = source;
        this.updatedTime = updatedTime;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    @JsonIgnore
    public InvitationMessageType getMessageType() {
        return InvitationMessageType.INVITATION_NOT_ACCEPTED;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public void setRecipientIdentifier(final String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    public InvitationSource getSource() {
        return source;
    }

    public void setSource(final InvitationSource source) {
        this.source = source;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
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
        final InvitationNotAcceptedMessage rhs = (InvitationNotAcceptedMessage) obj;
        return new EqualsBuilder()
                .append(recipientIdentifier, rhs.recipientIdentifier)
                .append(source, rhs.source)
                .append(updatedTime, rhs.updatedTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(recipientIdentifier)
                .append(source)
                .append(updatedTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(recipientIdentifier)
                .append(source)
                .append(updatedTime)
                .toString();
    }
}
