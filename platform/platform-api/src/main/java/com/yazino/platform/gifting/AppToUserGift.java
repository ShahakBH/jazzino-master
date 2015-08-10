package com.yazino.platform.gifting;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.joda.time.DateTime;

import java.io.Serializable;

@JsonSerialize
public class AppToUserGift implements Serializable {
    private final String gameType;
    private final Long amount;
    private final String title;
    private final String description;
    private final Long promoId;
    private final DateTime expiry;


    public AppToUserGift(final Long promoId,
                         final String gameType,
                         final DateTime expiry,
                         final Long amount,
                         final String title,
                         final String description) {
        this.gameType = gameType;
        this.amount = amount;
        this.title = title;
        this.description = description;
        this.promoId = promoId;
        this.expiry = expiry;
    }


    public DateTime getExpiry() {
        return expiry;
    }

    public Long getAmount() {
        return amount;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getGameType() {
        return gameType;
    }

    public Long getPromoId() {
        return promoId;
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
        AppToUserGift rhs = (AppToUserGift) obj;
        return new EqualsBuilder()
                .append(this.gameType, rhs.gameType)
                .append(this.amount, rhs.amount)
                .append(this.title, rhs.title)
                .append(this.description, rhs.description)
                .append(this.promoId, rhs.promoId)
                .append(this.expiry, rhs.expiry)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(gameType)
                .append(amount)
                .append(title)
                .append(description)
                .append(promoId)
                .append(expiry)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("gameType", gameType)
                .append("amount", amount)
                .append("title", title)
                .append("description", description)
                .append("promoId", promoId)
                .append("expiry", expiry)
                .toString();
    }
}