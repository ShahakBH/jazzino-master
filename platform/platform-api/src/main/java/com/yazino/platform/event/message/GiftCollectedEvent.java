package com.yazino.platform.event.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.gifting.CollectChoice;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GiftCollectedEvent implements PlatformEvent, GiftingEvent {

    @JsonProperty("gid")
    private BigDecimal giftId;
    @JsonProperty("gChoice")
    private CollectChoice choice;
    @JsonProperty("gAmount")
    private BigDecimal giftAmount;
    @JsonProperty("sid")
    private BigDecimal sessionId;
    @JsonProperty("cTs")
    private DateTime collectTs;

    public GiftCollectedEvent() {
    }

    public GiftCollectedEvent(final BigDecimal giftId,
                              final CollectChoice choice,
                              final BigDecimal giftAmount,
                              final BigDecimal sessionId,
                              final DateTime collectTs) {
        this.giftId = giftId;
        this.choice = choice;
        this.giftAmount = giftAmount;
        this.sessionId = sessionId;
        this.collectTs = collectTs;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.GIFT_COLLECTED;
    }

    @Override
    public BigDecimal getGiftId() {
        return giftId;
    }

    public CollectChoice getChoice() {
        return choice;
    }

    public BigDecimal getGiftAmount() {
        return giftAmount;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public DateTime getCollectTs() {
        return collectTs;
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
        GiftCollectedEvent rhs = (GiftCollectedEvent) obj;
        return new EqualsBuilder()
                .append(this.giftId, rhs.giftId)
                .append(this.choice, rhs.choice)
                .append(this.collectTs, rhs.collectTs)
                .isEquals()
                && this.giftAmount.compareTo(rhs.giftAmount) == 0
                && this.sessionId.compareTo(rhs.sessionId) == 0;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(giftId)
                .append(choice)
                .append(giftAmount.stripTrailingZeros())
                .append(sessionId.stripTrailingZeros())
                .append(collectTs)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("giftId", giftId)
                .append("choice", choice)
                .append("giftAmount", giftAmount)
                .append("sessionId", sessionId)
                .append("collectTs", collectTs)
                .toString();
    }
}
