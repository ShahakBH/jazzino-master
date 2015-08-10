package com.yazino.engagement.campaign.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newLinkedHashMap;

public class PlayerWithContent {

    private BigDecimal playerId;

    private Map<String, String> content = newLinkedHashMap();

    public void setContent(final Map<String, String> content) {
        this.content = content;
    }


    public PlayerWithContent() {
    }

    public PlayerWithContent(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Map<String, String> getContent() {
        return content;
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
        PlayerWithContent rhs = (PlayerWithContent) obj;
        return new EqualsBuilder()
                .append(this.content, rhs.content)
                .isEquals() && this.getPlayerId().compareTo(rhs.getPlayerId()) == 0;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(playerId.stripTrailingZeros())
                .append(content)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerId", playerId.stripTrailingZeros())
                .append("content", content)
                .toString();
    }
}
