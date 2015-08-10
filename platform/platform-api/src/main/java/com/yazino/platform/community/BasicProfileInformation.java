package com.yazino.platform.community;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class BasicProfileInformation implements Serializable {
    private static final long serialVersionUID = -8492997845633136792L;

    private final BigDecimal playerId;
    private final String name;
    private final String pictureUrl;
    private final BigDecimal accountId;

    public BasicProfileInformation(final BigDecimal playerId,
                                   final String name,
                                   final String pictureUrl,
                                   final BigDecimal accountId) {
        this.playerId = playerId;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.accountId = accountId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public BasicProfileInformation withPictureUrl(final String newPictureUrl) {
        return new BasicProfileInformation(playerId, name, newPictureUrl, accountId);
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
        final BasicProfileInformation rhs = (BasicProfileInformation) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(pictureUrl, rhs.pictureUrl)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(name)
                .append(pictureUrl)
                .append(BigDecimals.strip(accountId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(name)
                .append(pictureUrl)
                .append(accountId)
                .toString();
    }
}
