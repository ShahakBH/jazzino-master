package com.yazino.platform.event.message;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerEvent implements PlatformEvent, Serializable {
    private static final long serialVersionUID = 400307968487858004L;
    @JsonProperty("id")
    private BigDecimal playerId;
    @JsonProperty("ts")
    private DateTime tsCreated;
    @JsonProperty("acc")
    private BigDecimal accountId;
    @JsonProperty("tags")
    private Set<String> tags;

    private PlayerEvent() {
    }

    public PlayerEvent(final BigDecimal playerId,
                       final DateTime tsCreated,
                       final BigDecimal accountId,
                       final Set<String> tags) {
        notNull(playerId, "playerId is null");
        this.playerId = playerId;
        this.tsCreated = tsCreated;
        this.accountId = accountId;
        this.tags = tags;
    }

    @JsonProperty("insider")
    public Boolean isInsider() {
        // Only sent for compatibility with existing BI queries.
        return Boolean.FALSE;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.PLAYER;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public DateTime getTsCreated() {
        return tsCreated;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public Set<String> getTags() {
        return tags;
    }

    private void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    private void setTsCreated(final DateTime tsCreated) {
        this.tsCreated = tsCreated;
    }

    private void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    public void setTags(final Set<String> tags) {
        this.tags = tags;
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
        final PlayerEvent rhs = (PlayerEvent) obj;
        return new EqualsBuilder()
                .append(tsCreated, rhs.tsCreated)
                .append(tags, rhs.tags)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(BigDecimals.strip(playerId))
                .append(tsCreated)
                .append(tags)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "PlayerEvent{"
                + "playerId='" + playerId + '\''
                + ", tsCreated='" + tsCreated + '\''
                + '}';
    }
}
