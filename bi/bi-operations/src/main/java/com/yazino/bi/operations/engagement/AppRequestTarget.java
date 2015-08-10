package com.yazino.bi.operations.engagement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;

public class AppRequestTarget {

    private Integer id;
    private Integer campaignId;
    private BigDecimal playerId;
    private String externalId;
    private String gameType;

    public AppRequestTarget(final Integer id,
                            final Integer campaignId,
                            final BigDecimal playerId,
                            final String externalId,
                            final String gameType) {
        this.id = id;
        this.campaignId = campaignId;
        this.playerId = playerId;
        this.externalId = externalId;
        this.gameType = gameType;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(final Integer campaignId) {
        this.campaignId = campaignId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }


    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AppRequestTarget)) {
            return false;
        }
        final AppRequestTarget castOther = (AppRequestTarget) other;
        return new EqualsBuilder()
                .append(id, castOther.id)
                .append(campaignId, castOther.campaignId)
                .append(externalId, castOther.externalId)
                .append(gameType, castOther.gameType)
                .append(playerId, castOther.playerId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(campaignId)
                .append(externalId)
                .append(gameType)
                .append(playerId)
                .toHashCode();
    }
}
