package com.yazino.platform.invitation.persistence;

import com.yazino.platform.event.message.InvitationEvent;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.InvitationStatus;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class Invitation implements Serializable {
    private static final long serialVersionUID = -4153556103117986312L;

    private final BigDecimal issuingPlayerId;
    private final String recipientIdentifier;
    private final InvitationSource source;
    private InvitationStatus status;
    private Long rewardAmount;
    private final DateTime createTime;
    private DateTime updateTime;
    private final String gameType;
    private final String screenSource;


    public Invitation(final BigDecimal issuingPlayerId,
                      final String recipientIdentifier,
                      final InvitationSource source,
                      final InvitationStatus status,
                      final DateTime createTime,
                      final String gameType,
                      final String screenSource) {
        this(issuingPlayerId,
                recipientIdentifier,
                source,
                status,
                null,
                createTime,
                createTime,
                gameType,
                screenSource);
    }

    public Invitation(final BigDecimal issuingPlayerId,
                      final String recipientIdentifier,
                      final InvitationSource source,
                      final InvitationStatus status,
                      final Long rewardAmount,
                      final DateTime createTime,
                      final DateTime updateTime,
                      final String gameType,
                      final String screenSource) {
        notNull(issuingPlayerId, "null issuingPlayerId");
        notBlank(recipientIdentifier, "blank recipientIdentifier");
        notNull(source, "null source");
        notNull(status, "null status");
        notNull(createTime, "null createTime");
        notNull(updateTime, "null updateTime");

        this.issuingPlayerId = issuingPlayerId;
        this.recipientIdentifier = recipientIdentifier;
        this.source = source;
        this.rewardAmount = rewardAmount;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.gameType = gameType;
        this.screenSource = screenSource;
    }

    public Long getRewardAmount() {
        return rewardAmount;
    }

    public String getScreenSource() {
        return screenSource;
    }

    public InvitationSource getSource() {
        return source;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public BigDecimal getIssuingPlayerId() {
        return issuingPlayerId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public DateTime getCreateTime() {
        return createTime;
    }

    public DateTime getUpdateTime() {
        return updateTime;
    }

    public String getGameType() {
        return gameType;
    }

    public void marksAsAccepted(final Long acceptedRewardAmount, final DateTime acceptedUpdateTime) {
        this.status = InvitationStatus.ACCEPTED;
        this.rewardAmount = acceptedRewardAmount;
        this.updateTime = acceptedUpdateTime;
    }

    public void acceptedOther(final DateTime updatedTime) {
        this.status = InvitationStatus.ACCEPTED_OTHER;
        this.updateTime = updatedTime;
    }

    public void notAccepted(final DateTime updatedTime) {
        this.status = InvitationStatus.NOT_ACCEPTED;
        this.updateTime = updatedTime;
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
                .append(rewardAmount, castOther.rewardAmount)
                .append(createTime, castOther.createTime)
                .append(updateTime, castOther.updateTime)
                .append(gameType, castOther.gameType)
                .append(screenSource, castOther.screenSource)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(issuingPlayerId)
                .append(recipientIdentifier)
                .append(source)
                .append(status)
                .append(rewardAmount)
                .append(createTime)
                .append(updateTime)
                .append(gameType)
                .append(screenSource)
                .toHashCode();

    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public InvitationEvent toEvent() {
        BigDecimal reward = null;
        if (rewardAmount != null) {
            reward = new BigDecimal(rewardAmount);
        }
        return new InvitationEvent(issuingPlayerId,
                recipientIdentifier,
                source,
                status.name(),
                reward,
                gameType,
                screenSource,
                createTime,
                updateTime);
    }
}
