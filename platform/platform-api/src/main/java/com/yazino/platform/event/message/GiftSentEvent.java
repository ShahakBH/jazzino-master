package com.yazino.platform.event.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GiftSentEvent implements PlatformEvent, GiftingEvent {

    @JsonProperty("gid")
    private BigDecimal giftId;
    @JsonProperty("gSndId")
    private BigDecimal sender;
    @JsonProperty("gRecId")
    private BigDecimal receiver;
    @JsonProperty("gExp")
    private DateTime expiry;
    @JsonProperty("gSent")
    private DateTime now;
    @JsonProperty("sid")
    private BigDecimal sessionId;

    public GiftSentEvent(final BigDecimal giftId,
                         final BigDecimal sender,
                         final BigDecimal receiver,
                         final DateTime expiry,
                         final DateTime now,
                         final BigDecimal sessionId) {

        this.giftId = giftId;
        this.sender = sender;
        this.receiver = receiver;
        this.expiry = expiry;
        this.now = now;
        this.sessionId = sessionId;
    }

    private GiftSentEvent() {
    }

    public void setGiftId(final BigDecimal giftId) {
        this.giftId = giftId;
    }

    public void setSender(final BigDecimal sender) {
        this.sender = sender;
    }

    public void setReceiver(final BigDecimal receiver) {
        this.receiver = receiver;
    }

    public void setExpiry(final DateTime expiry) {
        this.expiry = expiry;
    }

    public void setNow(final DateTime now) {
        this.now = now;
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

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public DateTime getNow() {
        return now;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.GIFT_SENT;
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
        GiftSentEvent rhs = (GiftSentEvent) obj;
        return new EqualsBuilder()
                .append(this.giftId, rhs.giftId)
                .append(this.expiry, rhs.expiry)
                .append(this.now, rhs.now)
                .isEquals()
                && BigDecimals.equalByComparison(this.giftId, rhs.giftId)
                && BigDecimals.equalByComparison(this.sender, rhs.sender)
                && BigDecimals.equalByComparison(this.receiver, rhs.receiver)
                && BigDecimals.equalByComparison(this.sessionId, rhs.sessionId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(giftId))
                .append(BigDecimals.strip(sender))
                .append(BigDecimals.strip(receiver))
                .append(expiry)
                .append(now)
                .append(sessionId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("giftId", giftId)
                .append("sender", sender)
                .append("receiver", receiver)
                .append("expiry", expiry)
                .append("now", now)
                .append("sessionId", sessionId)
                .toString();
    }
}