package com.yazino.web.domain;

import com.yazino.platform.tournament.TournamentRegistrationInfo;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTimeUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class UpcomingTournamentSummary implements Serializable {
    private static final long serialVersionUID = 7771719534918130111L;

    private final BigDecimal tournamentId;
    private final String name;
    private final String variationName;
    private final long millisToStart;
    private final boolean playerRegistered;
    private final boolean friendsRegistered;


    public UpcomingTournamentSummary(final BigDecimal playerId,
                                     final Set<BigDecimal> friends,
                                     final TournamentRegistrationInfo tournament) {
        notNull(playerId, "playerId may not be null");
        notNull(friends, "friends may not be null");
        notNull(tournament, "tournament may not be null");

        this.tournamentId = tournament.getTournamentId();
        this.name = tournament.getName();
        this.variationName = tournament.getVariationTemplateName();
        this.millisToStart = tournament.getStartTimeStamp().getMillis() - DateTimeUtils.currentTimeMillis();
        this.playerRegistered = playerId != null && tournament.isRegistered(playerId);
        this.friendsRegistered = tournament.countMatchingPlayersRegistered(friends) > 0;
    }

    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public String getName() {
        return name;
    }

    public String getVariationName() {
        return variationName;
    }

    public long getMillisToStart() {
        return millisToStart;
    }

    public boolean isPlayerRegistered() {
        return playerRegistered;
    }

    public boolean isFriendsRegistered() {
        return friendsRegistered;
    }

    public boolean inProgress() {
        return millisToStart < 0;
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
        UpcomingTournamentSummary rhs = (UpcomingTournamentSummary) obj;
        return new EqualsBuilder()
                .append(this.tournamentId, rhs.tournamentId)
                .append(this.name, rhs.name)
                .append(this.variationName, rhs.variationName)
                .append(this.millisToStart, rhs.millisToStart)
                .append(this.playerRegistered, rhs.playerRegistered)
                .append(this.friendsRegistered, rhs.friendsRegistered)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(tournamentId)
                .append(name)
                .append(variationName)
                .append(millisToStart)
                .append(playerRegistered)
                .append(friendsRegistered)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("tournamentId", tournamentId)
                .append("name", name)
                .append("variationName", variationName)
                .append("millisToStart", millisToStart)
                .append("playerRegistered", playerRegistered)
                .append("friendsRegistered", friendsRegistered)
                .toString();
    }
}
