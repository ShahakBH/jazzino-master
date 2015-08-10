package com.yazino.platform.model.tournament;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

public class TournamentPlayers implements Iterable<TournamentPlayer>, Serializable {
    private static final long serialVersionUID = -6822583047890768742L;

    private static final Logger LOG = LoggerFactory.getLogger(TournamentPlayers.class);

    private final Set<TournamentPlayer> players = new CopyOnWriteArraySet<TournamentPlayer>();

    public int size() {
        return players.size();
    }

    public void add(final TournamentPlayer tournamentPlayer) {
        notNull(tournamentPlayer, "Tournament Player may not be null");
        players.add(tournamentPlayer);
    }

    public void addAll(final Collection<TournamentPlayer> tournamentPlayers) {
        notNull(tournamentPlayers, "Tournament Players may not be null");
        players.addAll(tournamentPlayers);
    }

    public int size(final TournamentPlayerStatus status) {
        notNull(status, "Status may not be null");

        return Collections2.filter(players, new FindByStatus(status)).size();
    }

    public boolean contains(final BigDecimal playerId) {
        return Iterables.tryFind(players, new FindByPlayerId(playerId)).isPresent();
    }

    @Override
    public Iterator<TournamentPlayer> iterator() {
        return players.iterator();
    }

    public Set<BigDecimal> getPlayerIds() {
        final Collection<BigDecimal> playerIds = Collections2.transform(
                players, new Function<TournamentPlayer, BigDecimal>() {
            public BigDecimal apply(final TournamentPlayer tournamentPlayer) {
                return tournamentPlayer.getPlayerId();
            }
        });
        return new HashSet<BigDecimal>(playerIds);
    }

    public boolean removeByPlayerId(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");

        final TournamentPlayer player = getByPlayerId(playerId);
        return player != null && players.remove(player);
    }

    public TournamentPlayer getByPlayerId(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Looking for player %s, valid players are: %s", playerId, players));
        }

        final TournamentPlayer tournamentPlayer = Iterables.tryFind(
                players, new FindByPlayerId(playerId)).orNull();

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Looking for player %s, found %s", playerId, tournamentPlayer));
        }

        return tournamentPlayer;
    }

    public List<TournamentPlayer> getByMaxLeaderboard(final int maxLeaderboardPosition) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Listing top " + maxLeaderboardPosition + " players");
        }

        final List<TournamentPlayer> result = newArrayList(
                Collections2.filter(players, new FindByMaxLeaderboardPosition(maxLeaderboardPosition)));

        Collections.sort(result, TournamentPlayer.BY_LEADERBOARD);

        return result;
    }

    public List<TournamentPlayer> getByMaxLeaderboardIncludingPlayer(final int maxLeaderboardPosition,
                                                                     final BigDecimal playerId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Listing top " + maxLeaderboardPosition + " players");
        }

        notNull(playerId, "Player ID may not be null");

        final TournamentPlayer player = getByPlayerId(playerId);

        final List<TournamentPlayer> result = newArrayList(
                Collections2.filter(players, new FindByMaxLeaderboardPosition(maxLeaderboardPosition)));
        if (player != null && !result.contains(player) && player.getLeaderboardPosition() != null) {
            result.add(player);
        }

        Collections.sort(result, TournamentPlayer.BY_LEADERBOARD);

        return result;
    }

    public Set<TournamentPlayer> getByStatus(final TournamentPlayerStatus status) {
        notNull(status, "Status may not be null");

        return new HashSet<TournamentPlayer>(Collections2.filter(players, new FindByStatus(status)));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(players)
                .toString();
    }

    private static class FindByPlayerId implements Predicate<TournamentPlayer> {
        private BigDecimal playerId;

        FindByPlayerId(final BigDecimal playerId) {
            notNull(playerId, "Player ID may not be null");
            this.playerId = playerId;
        }

        @Override
        public boolean apply(final TournamentPlayer candidatePlayer) {
            return playerId.compareTo(candidatePlayer.getPlayerId()) == 0;
        }
    }

    private static class FindByStatus implements Predicate<TournamentPlayer> {
        private TournamentPlayerStatus status;

        FindByStatus(final TournamentPlayerStatus status) {
            notNull(status, "Status may not be null");
            this.status = status;
        }

        @Override
        public boolean apply(final TournamentPlayer candidatePlayer) {
            return status == candidatePlayer.getStatus();
        }
    }

    private static class FindByMaxLeaderboardPosition implements Predicate<TournamentPlayer> {
        private int maxLeaderboardPosition;

        FindByMaxLeaderboardPosition(final int maxLeaderboardPosition) {
            this.maxLeaderboardPosition = maxLeaderboardPosition;
        }

        @Override
        public boolean apply(final TournamentPlayer candidatePlayer) {
            return candidatePlayer.getLeaderboardPosition() != null
                    && candidatePlayer.getLeaderboardPosition() <= maxLeaderboardPosition;
        }
    }

}
