package com.yazino.platform.gifting;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;


@JsonSerialize
public class Gift implements Serializable {
    private final BigDecimal giftId;
    private final BigDecimal sender;
    private final BigDecimal receiver;
    private final DateTime expiry;
    private final boolean acknowledged;

    public Gift(BigDecimal giftId, BigDecimal sender, BigDecimal receiver, DateTime expiry, boolean acknowledged) {
        this.giftId = giftId;
        this.sender = sender;
        this.receiver = receiver;
        this.expiry = expiry;
        this.acknowledged = acknowledged;
    }

    public BigDecimal getGiftId() {
        return giftId;
    }

    public BigDecimal getSender() {
        return sender;
    }

    public BigDecimal getReceiver() {
        return receiver;
    }

    public DateTime getExpiry() {
        return expiry;
    }

    public boolean isAcknowledged() {
        return acknowledged;
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
                .append(this.giftId, rhs.giftId)
                .append(this.sender, rhs.sender)
                .append(this.receiver, rhs.receiver)
                .append(this.expiry, rhs.expiry)
                .append(this.acknowledged, rhs.acknowledged)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(giftId)
                .append(sender)
                .append(receiver)
                .append(expiry)
                .append(acknowledged)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("giftId", giftId)
                .append("sender", sender)
                .append("receiver", receiver)
                .append("expiry", expiry)
                .append("acknowledged", acknowledged)
                .toString();
    }
}
