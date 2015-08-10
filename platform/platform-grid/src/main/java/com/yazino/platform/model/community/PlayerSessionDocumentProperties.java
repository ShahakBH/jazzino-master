package com.yazino.platform.model.community;

import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.session.Location;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * this class is used to build player location documents for community notifications
 * from sessions.
 */
public class PlayerSessionDocumentProperties {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerSessionDocumentProperties.class);

    public static final PlayerSessionDocumentProperties OFFLINE = new PlayerSessionDocumentProperties();

    private final boolean online;
    private final String nickname;
    private final String pictureUrl;
    private final BigDecimal balanceSnapshot;
    private final Set<Location> locations = new HashSet<>();

    public PlayerSessionDocumentProperties(final BigDecimal playerId,
                                           final PlayerSessionsSummary sessionsSummary,
                                           final TableInviteRepository tableInviteRepository) {
        this.online = true;
        this.nickname = sessionsSummary.getNickname();
        this.pictureUrl = sessionsSummary.getPictureUrl();
        this.balanceSnapshot = sessionsSummary.getBalanceSnapshot();

        if (playerId != null && tableInviteRepository != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checking visible tables for player " + playerId);
            }

            for (Location location : sessionsSummary.getLocations()) {
                LOG.debug("Location {}. Private? {}, Tournament? {}", location.getLocationId(), location.isPrivateLocation(), location.isTournamentLocation());
                if (location.isTournamentLocation()) {
                    continue;
                }
                if (!location.isPrivateLocation()) {
                    LOG.debug("Adding location {} to document", location.getLocationId());
                    locations.add(location);
                } else if (playerId.equals(location.getOwnerId())
                        || tableInviteRepository.findByTableAndPlayerId(
                        new BigDecimal(location.getLocationId()), playerId) != null) {
                    LOG.debug("Player {} has invite to table. Adding location {} to document",
                              playerId, location.getLocationId());
                    locations.add(location);
                }
            }
        } else {
            if (sessionsSummary.getLocations() != null) {
                this.locations.addAll(sessionsSummary.getLocations());
            }
        }
    }

    private PlayerSessionDocumentProperties() {
        this.online = false;
        this.nickname = null;
        this.pictureUrl = null;
        this.balanceSnapshot = null;
    }

    public boolean isOnline() {
        return online;
    }

    // used in document
    @SuppressWarnings("UnusedDeclaration")
    public Set<Location> getLocations() {
        return locations;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getNickname() {
        return nickname;
    }

    // used in document
    @SuppressWarnings("UnusedDeclaration")
    public BigDecimal getBalanceSnapshot() {
        return balanceSnapshot;
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

        final PlayerSessionDocumentProperties rhs = (PlayerSessionDocumentProperties) obj;
        return new EqualsBuilder()
                .append(online, rhs.online)
                .append(locations, rhs.locations)
                .append(pictureUrl, rhs.pictureUrl)
                .append(nickname, rhs.nickname)
                .append(balanceSnapshot, rhs.balanceSnapshot)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 43)
                .append(online)
                .append(locations)
                .append(nickname)
                .append(balanceSnapshot)
                .append(pictureUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
