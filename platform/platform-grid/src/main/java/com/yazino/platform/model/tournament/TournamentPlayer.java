package com.yazino.platform.model.tournament;

import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.processor.tournament.TournamentPlayerStatisticPublisher;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentPlayer implements Serializable, Comparable<TournamentPlayer>, Comparator<TournamentPlayer> {
    private static final long serialVersionUID = 5637093196944584252L;
    private static final Logger LOG = LoggerFactory.getLogger(TournamentPlayer.class);
    public static final Comparator<TournamentPlayer> BY_LEADERBOARD = new LeaderboardPositionComparator();

    private static final int MAX_POSITION_TO_DISPLAY = 20;

    public enum EliminationReason {
        OFFLINE, NOT_ENOUGH_CHIPS_FOR_ROUND, NO_CHIPS, KICKED_OUT_BY_GAME
    }

    private final Map<String, String> properties = new HashMap<>();

    private BigDecimal playerId;
    private BigDecimal tableId;
    private BigDecimal accountId;
    private Integer leaderboardPosition;
    private TournamentPlayerStatus status;
    private DateTime eliminationTimestamp;
    private EliminationReason eliminationReason;
    private BigDecimal settledPrize;
    private String name;
    private BigDecimal stack;

    public TournamentPlayer() {
    }

    public TournamentPlayer(final BigDecimal playerId, final String name) {
        notNull(playerId, "Player ID may not be null");
        notNull(name, "player name may not be null");
        this.playerId = playerId;
        this.name = name;
    }

    public TournamentPlayer(final BigDecimal playerId,
                            final String name,
                            final BigDecimal accountId,
                            final TournamentPlayerStatus status) {
        this(playerId, name, accountId, status, null);
    }

    public TournamentPlayer(final BigDecimal playerId,
                            final String name,
                            final BigDecimal accountId,
                            final TournamentPlayerStatus status,
                            final BigDecimal tableId) {
        this(playerId, name);

        notNull(accountId, "Account ID may not be null");
        notNull(status, "Tournament Player Status may not be null");

        this.accountId = accountId;
        this.status = status;
        this.tableId = tableId;
    }

    public TournamentPlayer(final TournamentPlayer tournamentPlayer) {
        notNull(tournamentPlayer, "Tournament Player may not be null");

        this.playerId = tournamentPlayer.playerId;
        this.accountId = tournamentPlayer.accountId;
        this.status = tournamentPlayer.status;
        this.tableId = tournamentPlayer.tableId;
        this.leaderboardPosition = tournamentPlayer.leaderboardPosition;
        this.name = tournamentPlayer.name;
        this.settledPrize = tournamentPlayer.settledPrize;
        this.eliminationTimestamp = tournamentPlayer.eliminationTimestamp;
        this.eliminationReason = tournamentPlayer.eliminationReason;
        this.stack = tournamentPlayer.stack;
    }

    /**
     * Change the status of the tournmanet player to the new value.
     *
     * @param newStatus      the new status. Not null.
     * @param tournamentHost the tournament host
     */
    public void changeStatus(final TournamentPlayerStatus newStatus, final TournamentHost tournamentHost) {
        if (newStatus == TournamentPlayerStatus.ELIMINATED) {
            throw new IllegalArgumentException("Use eliminate() instead");
        }
        changeStatus(newStatus, tournamentHost, null);
    }

    private void changeStatus(final TournamentPlayerStatus newStatus,
                              final TournamentHost tournamentHost,
                              final EliminationReason reason) {
        notNull(newStatus, "Status may not be null");
        notNull(tournamentHost, "Tournament host may not be null");
        LOG.debug("trying to set status to {} from {}", newStatus, status);
        if (newStatus != status) {
            if (status == null || status.isValidSuccessor(newStatus)) {
                if (!newStatus.isAccountRequired() && (status == null || status.isAccountRequired())) {
                    setStack(null);
                }
                if (!newStatus.canHaveTable()) {
                    tableId = null;
                }
                if (newStatus == TournamentPlayerStatus.ELIMINATED) {
                    eliminationTimestamp = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());
                    eliminationReason = reason;
                }
                LOG.debug("setting status to {} from {}", newStatus, status);
                this.status = newStatus;
            } else {
                throw new IllegalStateException(String.format("Status %s is not a valid successor status to %s", newStatus, status));
            }
        }
    }

    public void terminate(final TournamentHost tournamentHost,
                          final BigDecimal tournamentId,
                          final String gameType,
                          final int numberOfPlayers) {
        notNull(tournamentHost, "tournamentHost may not be null");
        notNull(tournamentId, "tournamentId may not be null");
        notNull(gameType, "gameType may not be null");

        changeStatus(TournamentPlayerStatus.TERMINATED, tournamentHost);

        LOG.debug("Player {} has been terminated from tournament {} at position {}", playerId, tournamentId, leaderboardPosition);
        tournamentHost.getTournamentRepository().playerEliminatedFrom(tournamentId, playerId, gameType, numberOfPlayers, leaderboardPosition);
    }

    public void eliminated(final TournamentHost tournamentHost,
                           final BigDecimal tournamentId,
                           final EliminationReason reason,
                           final String gameType,
                           final int numberOfPlayers) {
        notNull(tournamentHost, "tournamentHost may not be null");
        notNull(reason, "reason may not be null");
        notNull(gameType, "gameType may not be null");
        notNull(tournamentId, "tournamentId may not be null");

        changeStatus(TournamentPlayerStatus.ELIMINATED, tournamentHost, reason);

        LOG.debug("Player {} has been eliminated from tournament {} at position {} for {}",
                playerId, tournamentId, leaderboardPosition, reason);
        tournamentHost.getTournamentRepository().playerEliminatedFrom(tournamentId, playerId, gameType, numberOfPlayers, leaderboardPosition);
    }

    public void sendNewsIfRequired(final TournamentHost tournamentHost,
                                   final Tournament tournament) {
        final String gameType = tournament.getTournamentVariationTemplate().getGameType();
        final int numberOfPlayers = tournament.getPlayers().size();
        LOG.debug("Player {} - Publishing statistics if required status: {}, leaderboardPosition: {}, numberOfPlayers: {}",
                playerId, status, leaderboardPosition, numberOfPlayers);
        if (status == TournamentPlayerStatus.ELIMINATED || status == TournamentPlayerStatus.TERMINATED) {
            if (leaderboardPosition == null || leaderboardPosition > MAX_POSITION_TO_DISPLAY) {
                LOG.debug("Player {} - No statistic to publish", playerId);
                return;
            }
            final TournamentPlayerStatisticPublisher publisher = tournamentHost.getTournamentPlayerStatisticPublisher();
            final Map<String, String> statProperties = new HashMap<>();
            statProperties.put(TournamentStatisticProperty.POSITION.name(), String.valueOf(leaderboardPosition));
            statProperties.put(TournamentStatisticProperty.NUMBER_OF_PLAYERS.name(), String.valueOf(numberOfPlayers));
            final GameStatistic statistic = new GameStatistic(
                    playerId, TournamentStatisticType.FINAL_LEADERBOARD_POSITION.name(), statProperties);
            LOG.debug("Player {} - Publishing statistic {}", playerId, statistic);
            publisher.publishStatistics(playerId, gameType, Arrays.asList(statistic));
        }
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    public DateTime getEliminationTimestamp() {
        return eliminationTimestamp;
    }

    public void setEliminationTimestamp(final DateTime eliminationTimestamp) {
        this.eliminationTimestamp = eliminationTimestamp;
    }

    public Integer getLeaderboardPosition() {
        return leaderboardPosition;
    }

    public void setLeaderboardPosition(final Integer leaderboardPosition) {
        this.leaderboardPosition = leaderboardPosition;
    }

    public TournamentPlayerStatus getStatus() {
        return status;
    }

    public void setStatus(final TournamentPlayerStatus status) {
        this.status = status;
    }

    public BigDecimal getSettledPrize() {
        return settledPrize;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setSettledPrize(final BigDecimal settledPrize) {
        this.settledPrize = settledPrize;
    }

    public void setStack(final BigDecimal stack) {
        this.stack = stack;
    }

    public BigDecimal getStack() {
        return stack;
    }

    public EliminationReason getEliminationReason() {
        return eliminationReason;
    }

    public void setEliminationReason(final EliminationReason eliminationReason) {
        this.eliminationReason = eliminationReason;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public void setProperties(final Map<String, String> properties) {
        this.properties.clear();
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    public TournamentPlayerInfo toTournamentPlayerInfo(final Tournament tournament) {
        notNull(tournament, "Tournament may not be null");
        return new TournamentPlayerInfo(playerId, tournament.getTournamentId(), status);
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
        final TournamentPlayer tournamentPlayer = (TournamentPlayer) obj;
        return new EqualsBuilder()
                .append(status, tournamentPlayer.status)
                .append(name, tournamentPlayer.name)
                .append(settledPrize, tournamentPlayer.settledPrize)
                .append(eliminationTimestamp, tournamentPlayer.eliminationTimestamp)
                .append(eliminationReason, tournamentPlayer.eliminationReason)
                .append(properties, tournamentPlayer.properties)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, tournamentPlayer.tableId)
                && BigDecimals.equalByComparison(playerId, tournamentPlayer.playerId)
                && BigDecimals.equalByComparison(accountId, tournamentPlayer.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(BigDecimals.strip(playerId))
                .append(status)
                .append(BigDecimals.strip(tableId))
                .append(name)
                .append(settledPrize)
                .append(eliminationTimestamp)
                .append(eliminationReason)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int compareTo(final TournamentPlayer otherPlayer) {
        return BY_LEADERBOARD.compare(this, otherPlayer);
    }

    @Override
    public int compare(final TournamentPlayer player1, final TournamentPlayer player2) {
        return BY_LEADERBOARD.compare(player1, player2);
    }

    public static class LeaderboardPositionComparator implements Comparator<TournamentPlayer>, Serializable {
        private static final long serialVersionUID = 5006679661807358736L;

        @Override
        public int compare(final TournamentPlayer player1, final TournamentPlayer player2) {
            if (player1 == player2) {
                return 0;
            } else if (player1 == null) {
                return -1;
            } else if (player2 == null) {
                return 1;
            }

            return ObjectUtils.compare(player1.getLeaderboardPosition(), player2.getLeaderboardPosition());
        }
    }
}
