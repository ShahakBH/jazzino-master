package com.yazino.platform.invitation.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yazino.platform.invitation.InvitationSource;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.DateTime;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationSentMessage implements InvitationMessage {

    private static final long serialVersionUID = -5840917902902855385L;

    private static final int VERSION = 1;

    private BigDecimal issuingPlayerId;

    private String recipientIdentifier;

    private InvitationSource source;

    private DateTime createdTime;

    private String currentGame;

    private String screenSource;

    public InvitationSentMessage() {
    }

    public InvitationSentMessage(final BigDecimal issuingPlayerId,
                                 final String recipientIdentifier,
                                 final InvitationSource source,
                                 final DateTime createdTime,
                                 final String currentGame,
                                 final String screenSource) {
        notNull(issuingPlayerId, "issuingPlayerId may not be null");
        notBlank(recipientIdentifier, "recipientIdentifier may not be null or blank");
        notNull(source, "source may not be null");
        notNull(createdTime, "createdTime may not be null");

        this.issuingPlayerId = issuingPlayerId;
        this.recipientIdentifier = recipientIdentifier;
        this.source = source;
        this.createdTime = createdTime;
        this.currentGame = currentGame;
        this.screenSource = screenSource;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    @JsonIgnore
    public InvitationMessageType getMessageType() {
        return InvitationMessageType.INVITATION_SENT;
    }

    public InvitationSource getSource() {
        return source;
    }

    public void setSource(final InvitationSource source) {
        this.source = source;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public void setRecipientIdentifier(final String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    public BigDecimal getIssuingPlayerId() {
        return issuingPlayerId;
    }

    public void setIssuingPlayerId(final BigDecimal issuingPlayerId) {
        this.issuingPlayerId = issuingPlayerId;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getCurrentGame() {
        return currentGame;
    }

    public void setCurrentGame(final String currentGame) {
        this.currentGame = currentGame;
    }

    public String getScreenSource() {
        return screenSource;
    }

    public void setScreenSource(final String screenSource) {
        this.screenSource = screenSource;
    }

    public boolean isValid() {
        return issuingPlayerId != null
                && recipientIdentifier != null
                && source != null
                && createdTime != null;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof InvitationSentMessage)) {
            return false;
        }
        final InvitationSentMessage castOther = (InvitationSentMessage) other;
        return new EqualsBuilder()
                .append(issuingPlayerId, castOther.issuingPlayerId)
                .append(recipientIdentifier, castOther.recipientIdentifier)
                .append(source, castOther.source)
                .append(createdTime, castOther.createdTime)
                .append(currentGame, castOther.currentGame)
                .append(screenSource, castOther.screenSource)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(issuingPlayerId)
                .append(recipientIdentifier)
                .append(source)
                .append(createdTime)
                .append(currentGame)
                .append(screenSource)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("issuingPlayerId", issuingPlayerId)
                .append("recipientIdentifier", recipientIdentifier).append("source", source)
                .append("createdTime", createdTime).append("currentGame", currentGame)
                .append("screenSource", screenSource).toString();
    }
}
