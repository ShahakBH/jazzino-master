package com.yazino.platform.model.session;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.session.Location;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@SpaceClass
public class PlayerSession implements Serializable {
    private static final long serialVersionUID = 7055025681421975691L;

    private BigDecimal sessionId;
    private BigDecimal playerId;
    private String localSessionKey;
    private String pictureUrl;
    private String nickname;
    private Partner partnerId;
    private Platform platform;
    private String ipAddress;
    private Date timestamp;
    private Set<Location> locations;
    private Boolean playing;
    private BigDecimal balanceSnapshot;
    private String email;

    public PlayerSession() {
        // for gs
    }

    public PlayerSession(final BigDecimal playerId) {
        this.playerId = playerId; // for templates
    }

    public PlayerSession(final BigDecimal playerId,
                         final String sessionKey) {
        this.playerId = playerId; // for templates
        this.localSessionKey = sessionKey;
    }

    public PlayerSession(final BigDecimal sessionId,
                         final BigDecimal playerId,
                         final String localSessionKey,
                         final String pictureUrl,
                         final String nickname,
                         final Partner partnerId,
                         final Platform platform,
                         final String ipAddress,
                         final BigDecimal balanceSnapshot,
                         final String email) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.localSessionKey = localSessionKey;
        this.pictureUrl = pictureUrl;
        this.balanceSnapshot = balanceSnapshot;
        this.nickname = nickname;
        this.partnerId = partnerId;
        this.platform = platform;
        this.ipAddress = ipAddress;
        this.timestamp = new Date();
        this.email = email;

        initLocationsIfRequired();
    }

    @SpaceId
    public BigDecimal getSessionId() {
        return sessionId;
    }

    public void setSessionId(final BigDecimal sessionId) {
        this.sessionId = sessionId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    @SpaceIndex
    public String getLocalSessionKey() {
        return localSessionKey;
    }

    public void setLocalSessionKey(final String localSessionKey) {
        this.localSessionKey = localSessionKey;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(final String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    public Set<Location> getLocations() {
        return locations;
    }

    public void setLocations(final Set<Location> locations) {
        if (locations != null) {
            initLocationsIfRequired();
            this.locations.clear();
            this.locations.addAll(locations);

        } else {
            this.locations = null;
        }
    }

    private void initLocationsIfRequired() {
        // this isn't entirely thread safe, but the usage pattern should
        // mean we never double-init in practice
        if (locations == null) {
            locations = new CopyOnWriteArraySet<>();
        }
    }

    public void addLocation(final Location location) {
        initLocationsIfRequired();
        this.locations.add(location);
        updateIsPlaying();
    }

    public void removeLocation(final Location location) {
        if (locations == null) {
            return;
        }
        this.locations.remove(location);
        updateIsPlaying();
    }

    private void updateIsPlaying() {
        if (locations != null) {
            this.playing = this.locations.size() > 0;
        }
    }

    public String getNickname() {
        return nickname;
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    public void setNickname(final String nickname) {
        this.nickname = nickname;
    }

    public void setPartnerId(final Partner partnerId) {
        this.partnerId = partnerId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(final Platform platform) {
        this.platform = platform;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public BigDecimal getBalanceSnapshot() {
        return balanceSnapshot;
    }

    public void setBalanceSnapshot(final BigDecimal balanceSnapshot) {
        this.balanceSnapshot = balanceSnapshot;
    }

    public Boolean getPlaying() {
        return playing;
    }

    public void setPlaying(final Boolean playing) {
        this.playing = playing;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final PlayerSession rhs = (PlayerSession) obj;
        return new EqualsBuilder()
                .append(localSessionKey, rhs.localSessionKey)
                .append(pictureUrl, rhs.pictureUrl)
                .append(nickname, rhs.nickname)
                .append(partnerId, rhs.partnerId)
                .append(platform, rhs.platform)
                .append(ipAddress, rhs.ipAddress)
                .append(timestamp, rhs.timestamp)
                .append(locations, rhs.locations)
                .append(balanceSnapshot, rhs.balanceSnapshot)
                .append(email, rhs.email)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(sessionId, rhs.sessionId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(sessionId))
                .append(BigDecimals.strip(playerId))
                .append(localSessionKey)
                .append(pictureUrl)
                .append(nickname)
                .append(partnerId)
                .append(platform)
                .append(ipAddress)
                .append(timestamp)
                .append(locations)
                .append(balanceSnapshot)
                .append(email)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean hasPublicLocations(final String gameType) {
        if (locations == null) {
            return false;
        }
        for (Location location : locations) {
            if (!location.isPrivateLocation() && location.getGameType().equals(gameType)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> retrieveGameTypesFromPublicLocations() {
        final HashSet<String> result = new HashSet<>();
        if (locations == null) {
            return result;
        }
        for (Location location : locations) {
            if (!location.isPrivateLocation()) {
                result.add(location.getGameType());
            }
        }
        return result;
    }

    public Set<String> retrieveLocationIds() {
        final Set<String> result = new HashSet<>();
        if (locations != null) {
            for (Location location : locations) {
                result.add(location.getLocationId());
            }
        }
        return result;
    }
}
