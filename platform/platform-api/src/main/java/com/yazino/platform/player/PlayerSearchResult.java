package com.yazino.platform.player;

import com.google.common.base.Optional;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerSearchResult implements Serializable {
    private static final long serialVersionUID = -2786446046999320845L;

    private final BigDecimal playerId;
    private final String realName;
    private final String displayName;
    private final String providerName;
    private final String emailAddress;
    private final String avatarUrl;
    private final PlayerProfileStatus status;
    private final PlayerProfileRole role;

    public PlayerSearchResult(final BigDecimal playerId,
                              final String emailAddress,
                              final String realName,
                              final String displayName,
                              final String providerName,
                              final String avatarUrl,
                              final PlayerProfileStatus status,
                              final PlayerProfileRole role) {
        notNull(playerId, "playerId may not be null");
        notNull(providerName, "providerName may not be null");
        notNull(status, "status may not be null");
        notNull(role, "role may not be null");

        this.playerId = playerId;
        this.realName = realName;
        this.displayName = displayName;
        this.providerName = providerName;
        this.emailAddress = emailAddress;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.role = role;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Optional<String> getRealName() {
        return Optional.fromNullable(realName);
    }

    public Optional<String> getDisplayName() {
        return Optional.fromNullable(displayName);
    }

    public String getProviderName() {
        return providerName;
    }

    public Optional<String> getEmailAddress() {
        return Optional.fromNullable(emailAddress);
    }

    public Optional<String> getAvatarUrl() {
        return Optional.fromNullable(avatarUrl);
    }

    public PlayerProfileStatus getStatus() {
        return status;
    }

    public PlayerProfileRole getRole() {
        return role;
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
        final PlayerSearchResult rhs = (PlayerSearchResult) obj;
        return new EqualsBuilder()
                .append(realName, rhs.realName)
                .append(displayName, rhs.displayName)
                .append(providerName, rhs.providerName)
                .append(emailAddress, rhs.emailAddress)
                .append(avatarUrl, rhs.avatarUrl)
                .append(status, rhs.status)
                .append(role, rhs.role)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(realName)
                .append(displayName)
                .append(providerName)
                .append(emailAddress)
                .append(avatarUrl)
                .append(status)
                .append(role)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(realName)
                .append(displayName)
                .append(providerName)
                .append(emailAddress)
                .append(avatarUrl)
                .append(status)
                .append(role)
                .toString();
    }
}
