package com.yazino.platform.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class Session implements Serializable {
    private static final long serialVersionUID = 6794525772251161924L;

    private final Set<Location> locations = new HashSet<>();
    private final Set<String> tags = new HashSet<>();

    private final BigDecimal sessionId;
    private final BigDecimal playerId;
    private final Partner partnerId;
    private final Platform platform;
    private final String ipAddress;
    private final String localSessionKey;
    private final String pictureUrl;
    private final String nickname;
    private final String email;
    private final BigDecimal balanceSnapshot;
    private final DateTime timestamp;

    public Session(final BigDecimal sessionId,
                   final BigDecimal playerId,
                   final Partner partnerId,
                   final Platform platform,
                   final String ipAddress,
                   final String localSessionKey,
                   final String nickname,
                   final String email,
                   final String pictureUrl,
                   final BigDecimal balanceSnapshot,
                   final DateTime timestamp,
                   final Collection<Location> locations,
                   final Collection<String> tags) {
        notNull(playerId, "playerId may not be null");

        this.playerId = playerId;
        this.sessionId = sessionId;
        this.partnerId = partnerId;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.localSessionKey = localSessionKey;
        this.pictureUrl = pictureUrl;
        this.nickname = nickname;
        this.email = email;
        this.timestamp = timestamp;
        this.balanceSnapshot = balanceSnapshot;

        if (locations != null) {
            this.locations.addAll(locations);
        }
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public Set<Location> getLocations() {
        return Collections.synchronizedSet(locations);
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getLocalSessionKey() {
        return localSessionKey;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public BigDecimal getBalanceSnapshot() {
        return balanceSnapshot;
    }

    public Set<String> getTags() {
        return tags;
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
        final Session rhs = (Session) obj;
        return new EqualsBuilder()
                .append(partnerId, rhs.partnerId)
                .append(platform, rhs.platform)
                .append(ipAddress, rhs.ipAddress)
                .append(localSessionKey, rhs.localSessionKey)
                .append(email, rhs.email)
                .append(nickname, rhs.nickname)
                .append(pictureUrl, rhs.pictureUrl)
                .append(balanceSnapshot, rhs.balanceSnapshot)
                .append(timestamp, rhs.timestamp)
                .append(locations, rhs.locations)
                .append(tags, rhs.tags)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(sessionId, rhs.sessionId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(sessionId))
                .append(BigDecimals.strip(playerId))
                .append(partnerId)
                .append(platform)
                .append(ipAddress)
                .append(localSessionKey)
                .append(email)
                .append(nickname)
                .append(pictureUrl)
                .append(balanceSnapshot)
                .append(timestamp)
                .append(locations)
                .append(tags)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(sessionId)
                .append(playerId)
                .append(partnerId)
                .append(platform)
                .append(ipAddress)
                .append(localSessionKey)
                .append(email)
                .append(nickname)
                .append(pictureUrl)
                .append(balanceSnapshot)
                .append(timestamp)
                .append(locations)
                .append(tags)
                .toString();
    }
}
