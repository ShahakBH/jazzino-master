package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class Gift implements Serializable {
    private static final long serialVersionUID = -3551114889088391660L;

    private BigDecimal id;
    private BigDecimal sendingPlayer;
    private BigDecimal recipientPlayer;
    private DateTime created;
    private DateTime expiry;
    private DateTime collected;
    private Boolean acknowledged;

    public Gift() {
        // for GS templates
    }

    public Gift(final BigDecimal recipientPlayer) {
        notNull(recipientPlayer, "recipientPlayer may not be null");

        this.recipientPlayer = recipientPlayer;
    }

    public Gift(final BigDecimal id,
                final BigDecimal sendingPlayer,
                final BigDecimal recipientPlayer,
                final DateTime created,
                final DateTime expiry,
                final DateTime collected,
                final Boolean acknowledged) {
        notNull(id, "id may not be null");
        notNull(sendingPlayer, "sendingPlayer may not be null");
        notNull(recipientPlayer, "recipientPlayer may not be null");
        notNull(created, "created may not be null");
        notNull(expiry, "expiry may not be null");
        notNull(acknowledged, "acknowledged may not be null");

        this.id = id;
        this.sendingPlayer = sendingPlayer;
        this.recipientPlayer = recipientPlayer;
        this.created = created;
        this.expiry = expiry;
        this.collected = collected;
        this.acknowledged = acknowledged;
    }

    @SpaceId
    public BigDecimal getId() {
        return id;
    }

    public void setId(final BigDecimal id) {
        this.id = id;
    }

    public BigDecimal getSendingPlayer() {
        return sendingPlayer;
    }

    @SpaceIndex
    public void setSendingPlayer(final BigDecimal sendingPlayer) {
        this.sendingPlayer = sendingPlayer;
    }

    @SpaceRouting
    public BigDecimal getRecipientPlayer() {
        return recipientPlayer;
    }

    public void setRecipientPlayer(final BigDecimal recipientPlayer) {
        this.recipientPlayer = recipientPlayer;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(final DateTime created) {
        this.created = created;
    }

    public DateTime getExpiry() {
        return expiry;
    }

    public void setExpiry(final DateTime expiry) {
        this.expiry = expiry;
    }

    public DateTime getCollected() {
        return collected;
    }

    public void setCollected(final DateTime collected) {
        this.collected = collected;
    }

    public Boolean getAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(final Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    @SpaceIndex
    public Long getCreatedInMillis() {
        if (created != null) {
            return created.getMillis();
        }
        return null;
    }

    public void setCreatedInMillis(final Long createdInMillis) {
        // for GS conventions only
    }

    @SpaceIndex
    public Long getCollectedInMillis() {
        if (collected != null) {
            return collected.getMillis();
        }
        return null;
    }

    public void setCollectedInMillis(final Long collectedInMillis) {
        // for GS conventions only
    }

    @SpaceIndex
    public Long getExpiryInMillis() {
        if (expiry != null) {
            return expiry.getMillis();
        }
        return null;
    }

    public void setExpiryInMillis(final Long expiryInMillis) {
        // for GS conventions only
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Gift rhs = (Gift) obj;
        return new EqualsBuilder()
                .append(this.created, rhs.created)
                .append(this.expiry, rhs.expiry)
                .append(this.collected, rhs.collected)
                .append(this.acknowledged, rhs.acknowledged)
                .isEquals()
                && BigDecimals.equalByComparison(id, rhs.id)
                && BigDecimals.equalByComparison(sendingPlayer, rhs.sendingPlayer)
                && BigDecimals.equalByComparison(recipientPlayer, rhs.recipientPlayer);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(id))
                .append(BigDecimals.strip(sendingPlayer))
                .append(BigDecimals.strip(recipientPlayer))
                .append(created)
                .append(expiry)
                .append(collected)
                .append(acknowledged)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("sendingPlayer", sendingPlayer)
                .append("recipientPlayer", recipientPlayer)
                .append("created", created)
                .append("expiry", expiry)
                .append("collected", collected)
                .append("acknowledged", acknowledged)
                .toString();
    }
}
