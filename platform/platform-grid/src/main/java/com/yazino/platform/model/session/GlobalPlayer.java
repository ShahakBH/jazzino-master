package com.yazino.platform.model.session;

import com.yazino.platform.session.InvalidPlayerSessionException;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.PlayerLocations;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class GlobalPlayer implements Serializable, Comparable<GlobalPlayer> {
    private static final long serialVersionUID = -9088705896670464012L;

    private BigDecimal playerId;
    private String gameType;
    private String nickname;
    private String pictureUrl;
    private BigDecimal balanceSnapshot;
    private Location location;

    public GlobalPlayer(final String gameType,
                        final BigDecimal playerId,
                        final String nickname,
                        final String pictureUrl,
                        final BigDecimal balanceSnapshot,
                        final Set<Location> locations)
            throws InvalidPlayerSessionException {
        notNull(gameType, "gameType is null");
        notNull(playerId, "playerId is null");

        this.gameType = gameType;
        this.playerId = playerId;
        this.nickname = nickname;
        this.pictureUrl = pictureUrl;
        this.balanceSnapshot = balanceSnapshot;

        if (locations != null) {
            for (Location currentLocation : locations) {
                if (!currentLocation.isPrivateLocation() && currentLocation.getGameType().equals(gameType)) {
                    this.location = currentLocation;
                    break;
                }
            }
        }
        if (this.location == null) {
            throw new InvalidPlayerSessionException(
                    "Player session does not contain a valid location for gameType " + gameType);
        }
    }

    public PlayerLocations toLocations() {
        return new PlayerLocations(playerId, Arrays.asList(new BigDecimal(location.getLocationId())));
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    @Override
    public int compareTo(final GlobalPlayer other) {
        return playerId.compareTo(other.getPlayerId());
    }

    public Location getLocation() {
        return location;
    }

    public String getGameType() {
        return gameType;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public BigDecimal getBalanceSnapshot() {
        return balanceSnapshot;
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
        final GlobalPlayer rhs = (GlobalPlayer) obj;
        return BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(gameType)
                .append(nickname)
                .append(pictureUrl)
                .append(balanceSnapshot)
                .append(location)
                .toString();
    }
}
