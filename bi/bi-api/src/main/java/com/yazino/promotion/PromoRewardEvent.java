package com.yazino.promotion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.messaging.Message;
import org.joda.time.DateTime;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class PromoRewardEvent implements Message<String> {

    private PromoRewardEvent() {
    }

    public PromoRewardEvent(final BigDecimal playerId, final Long promoId, final DateTime activity) {
        notNull(playerId, "playerId may not be null");
        notNull(promoId, "promoId may not be null");
        notNull(activity, "activity may not be null");
        this.playerId = playerId;
        this.promoId = promoId;
        this.activity = activity;
    }

    @JsonProperty("id")
    private BigDecimal playerId;


    @JsonProperty("pro")
    private Long promoId;

    @JsonProperty("act")
    private DateTime activity;

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public Long getPromoId() {
        return promoId;
    }

    public void setPromoId(final Long promoId) {
        this.promoId = promoId;
    }

    public DateTime getActivityTime() {
        return activity;
    }

    public void setActivity(final DateTime activity) {
        this.activity = activity;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public String getMessageType() {
        return "PROMO_REWARD";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PromoRewardEvent that = (PromoRewardEvent) o;

        if (!activity.equals(that.activity)) {
            return false;
        }
        if (playerId.compareTo(that.playerId) != 0) {
            return false;
        }
        if (!promoId.equals(that.promoId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = playerId.hashCode();
        result = 31 * result + promoId.hashCode();
        result = 31 * result + activity.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PromoRewardEvent{"
                + "playerId=" + playerId
                + ", promoId=" + promoId
                + ", activity=" + activity
                + '}';
    }
}
