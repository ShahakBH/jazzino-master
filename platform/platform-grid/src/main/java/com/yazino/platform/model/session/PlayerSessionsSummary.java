package com.yazino.platform.model.session;

import com.yazino.platform.session.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class PlayerSessionsSummary implements Serializable {
    private static final long serialVersionUID = -5489903267650803377L;

    private final Set<Location> locations = new HashSet<>();

    private final String nickname;
    private final String pictureUrl;
    private final BigDecimal balanceSnapshot;


    public PlayerSessionsSummary(final String nickname,
                                 final String pictureUrl,
                                 final BigDecimal balanceSnapshot,
                                 final Set<Location> locations) {
        this.nickname = nickname;
        this.pictureUrl = pictureUrl;
        this.balanceSnapshot = balanceSnapshot;

        if (locations != null) {
            this.locations.addAll(locations);
        }
    }

    public void addLocations(final Set<Location> additionalLocations) {
        if (additionalLocations != null) {
            this.locations.addAll(additionalLocations);
        }
    }

    public Set<Location> getLocations() {
        return locations;
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
        PlayerSessionsSummary rhs = (PlayerSessionsSummary) obj;
        return new EqualsBuilder()
                .append(this.locations, rhs.locations)
                .append(this.nickname, rhs.nickname)
                .append(this.pictureUrl, rhs.pictureUrl)
                .append(this.balanceSnapshot, rhs.balanceSnapshot)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(locations)
                .append(nickname)
                .append(pictureUrl)
                .append(balanceSnapshot)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("locations", locations)
                .append("nickname", nickname)
                .append("pictureUrl", pictureUrl)
                .append("balanceSnapshot", balanceSnapshot)
                .toString();
    }
}
