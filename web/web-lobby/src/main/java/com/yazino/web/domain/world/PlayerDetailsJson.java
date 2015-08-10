package com.yazino.web.domain.world;

import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.web.domain.LocationDetails;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlayerDetailsJson implements Serializable {

    private static final long serialVersionUID = -5111001184388727981L;

    private final boolean online;
    private final String nickname;
    private final String pictureUrl;
    private final BigDecimal balanceSnapshot;
    private final Map<String, Integer> levels;
    private final Set<LocationDetails> locations;

    private PlayerDetailsJson(final BasicProfileInformation playerDetails,
                              final BigDecimal balanceSnapshot,
                              final Map<String, Integer> levels,
                              final Set<LocationDetails> locationDetails) {
        this.online = true;
        this.nickname = playerDetails.getName();
        this.pictureUrl = playerDetails.getPictureUrl();
        this.balanceSnapshot = balanceSnapshot;
        this.locations = locationDetails;
        this.levels = levels;
    }

    public boolean isOnline() {
        return online;
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

    public Set<LocationDetails> getLocations() {
        return locations;
    }

    public Map<String, Integer> getLevels() {
        return levels;
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
        final PlayerDetailsJson rhs = (PlayerDetailsJson) obj;
        return new EqualsBuilder()
                .append(online, rhs.online)
                .append(nickname, rhs.nickname)
                .append(pictureUrl, rhs.pictureUrl)
                .append(balanceSnapshot, rhs.balanceSnapshot)
                .append(locations, rhs.locations)
                .append(levels, rhs.levels)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(online)
                .append(nickname)
                .append(pictureUrl)
                .append(balanceSnapshot)
                .append(locations)
                .append(levels)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "PlayerDetailsJson{"
                + "online=" + online
                + ", nickname='" + nickname + '\''
                + ", pictureUrl='" + pictureUrl + '\''
                + ", balanceSnapshot=" + balanceSnapshot
                + ", levels=" + levels
                + ", locations=" + locations
                + '}';
    }

    public static class Builder {
        private BasicProfileInformation basicProfileInformation;
        private BigDecimal balanceSnapshot;
        private Map<String, Integer> levels = new HashMap<String, Integer>();
        private Set<LocationDetails> locations = new HashSet<LocationDetails>();

        public Builder(final BasicProfileInformation basicProfileInformation,
                       final BigDecimal balanceSnapshot) {
            this.basicProfileInformation = basicProfileInformation;
            this.balanceSnapshot = balanceSnapshot;
        }

        public Builder addLevel(final String gameType,
                                final Integer level) {
            levels.put(gameType, level);
            return this;
        }

        public Builder addLocation(final LocationDetails location) {
            locations.add(location);
            return this;
        }

        public PlayerDetailsJson build() {
            return new PlayerDetailsJson(basicProfileInformation, balanceSnapshot, levels, locations);
        }
    }
}
