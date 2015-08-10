package com.yazino.platform.event.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationEvent implements PlatformEvent {
    private static final long serialVersionUID = 6167832921124804990L;

    private BigDecimal issuingPlayerId;
    private String recipientIdentifier;
    private InvitationSource source;
    private String status;
    private BigDecimal reward;
    private String gameType;
    private String screenSource;
    private DateTime createdTime;
    private DateTime updatedTime;

    public InvitationEvent() {
    }

    public InvitationEvent(final BigDecimal issuingPlayerId,
                           final String recipientIdentifier,
                           final InvitationSource source,
                           final String status,
                           final BigDecimal reward,
                           final String gameType,
                           final String screenSource,
                           final DateTime createdTime,
                           final DateTime updatedTime) {
        this.issuingPlayerId = issuingPlayerId;
        this.recipientIdentifier = recipientIdentifier;
        this.source = source;
        this.status = status;
        this.reward = reward;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
        this.gameType = gameType;
        this.screenSource = screenSource;
    }

    public BigDecimal getIssuingPlayerId() {
        return issuingPlayerId;
    }

    public void setIssuingPlayerId(final BigDecimal issuingPlayerId) {
        this.issuingPlayerId = issuingPlayerId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public BigDecimal getReward() {
        return reward;
    }

    public void setReward(final BigDecimal reward) {
        this.reward = reward;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getScreenSource() {
        return screenSource;
    }

    public void setScreenSource(final String screenSource) {
        this.screenSource = screenSource;
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.INVITATION;
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
        final InvitationEvent rhs = (InvitationEvent) obj;
        return new EqualsBuilder()
                .append(issuingPlayerId, rhs.issuingPlayerId)
                .append(recipientIdentifier, rhs.recipientIdentifier)
                .append(source, rhs.source)
                .append(status, rhs.status)
                .append(reward, rhs.reward)
                .append(gameType, rhs.gameType)
                .append(screenSource, rhs.screenSource)
                .append(createdTime, rhs.createdTime)
                .append(updatedTime, rhs.updatedTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(issuingPlayerId)
                .append(recipientIdentifier)
                .append(source)
                .append(status)
                .append(reward)
                .append(gameType)
                .append(screenSource)
                .append(createdTime)
                .append(updatedTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(issuingPlayerId)
                .append(recipientIdentifier)
                .append(source)
                .append(status)
                .append(reward)
                .append(gameType)
                .append(screenSource)
                .append(createdTime)
                .append(updatedTime)
                .toString();
    }

}
