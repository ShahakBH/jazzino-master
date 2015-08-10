package com.yazino.platform.tournament;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class TrophyLeaderboardPlayers implements Serializable {
    private static final long serialVersionUID = -5161343706926058391L;
    private final List<TrophyLeaderboardPlayer> players = new LinkedList<TrophyLeaderboardPlayer>();

    public void addPlayer(final TrophyLeaderboardPlayer leaderboardPlayer) {
        players.add(leaderboardPlayer);
    }

    public void updatePlayersPositions() {
        Collections.sort(players, TrophyLeaderboardPlayer.SORT_BY_POINTS);
        int leaderboardPosition = 1;
        int index = 1;
        long lastPoints = 0;
        for (TrophyLeaderboardPlayer player : players) {
            final long playerPoints = player.getPoints();
            if (playerPoints != lastPoints) {
                lastPoints = playerPoints;
                leaderboardPosition = index;
            }
            player.setLeaderboardPosition(leaderboardPosition);
            ++index;
        }
    }

    public List<TrophyLeaderboardPlayer> getOrderedByPosition() {
        return players;
    }

    public Set<Integer> getPositions() {
        final Set<Integer> positions = new HashSet<Integer>();
        for (TrophyLeaderboardPlayer player : players) {
            positions.add(player.getLeaderboardPosition());
        }
        return positions;
    }

    public Set<TrophyLeaderboardPlayer> getPlayersOnPosition(final int position) {
        final Set<TrophyLeaderboardPlayer> positions = new HashSet<TrophyLeaderboardPlayer>();
        for (TrophyLeaderboardPlayer player : players) {
            if (player.getLeaderboardPosition() == position) {
                positions.add(player);
            }
        }
        return positions;
    }

    public TrophyLeaderboardPlayer findPlayer(final BigDecimal playerId) {
        try {
            return Iterables.find(players, new TrophyLeaderboardPlayer.MatchPlayerIdPredicate(playerId));
        } catch (NoSuchElementException e) {
            return null;
        }
    }


    public void clear() {
        players.clear();
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
        final TrophyLeaderboardPlayers rhs = (TrophyLeaderboardPlayers) obj;
        return new EqualsBuilder()
                .append(players, rhs.players)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(players)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(players)
                .toString();
    }
}
