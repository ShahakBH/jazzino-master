package com.yazino.platform.model.tournament;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentVariationRound;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.Validate.notNull;

public class TournamentLeaderboard implements Serializable {
    private static final long serialVersionUID = -8537142848945474322L;

    private static final Logger LOG = LoggerFactory.getLogger(TournamentLeaderboard.class);

    private static final int MINIMUM_ACTIVE_PLAYERS = 2;

    private final BigDecimal tournamentId;
    private List<TournamentPlayer> activePlayers;
    private Map<BigDecimal, BigDecimal> playerBalances;

    public TournamentLeaderboard(final BigDecimal tournamentId) {
        notNull(tournamentId, "Tournament id is required");
        this.tournamentId = tournamentId;
    }

    public boolean updateLeaderboard(final Tournament tournament,
                                     final TournamentHost tournamentHost,
                                     final boolean eliminateInactivePlayers,
                                     final boolean eliminatePlayersNotOnline) {
        if (isInvalidStatus(tournament.getTournamentStatus())) {
            return false;
        }
        LOG.debug("Updating leaderboard for tournament {}", tournament.getTournamentId());

        updatePlayersAndBalances(tournamentHost, tournament.retrievePlayers());

        if (eliminatePlayersNotOnline) {
            eliminatePlayersThatAreNotOnline(tournamentHost, tournament);
        }

        if (tournament.getTournamentStatus() == TournamentStatus.ON_BREAK) {
            eliminateUnderMinimumBalance(tournament, tournamentHost);
        }

        if (eliminateInactivePlayers) {
            eliminatePlayersInactiveAtTables(tournament, tournamentHost);
        }

        updateLeaderboardPositions(tournament, tournamentHost);

        if (LOG.isDebugEnabled()) {
            logCurrentPlayerStatus(tournament);
        }

        return isInsufficientPlayersPresent();
    }

    private void updateLeaderboardPositions(final Tournament tournament,
                                            final TournamentHost tournamentHost) {
        Collections.sort(activePlayers, new LeaderboardComparator());

        int leaderboardPosition = 1;
        int index = 1;
        BigDecimal lastPoints = BigDecimal.ZERO;
        TournamentPlayerStatus lastStatus = TournamentPlayerStatus.ACTIVE;
        LOG.debug("Updating leaderboard positions for tournament {}: Number of players is {} for tournament status {}",
                tournamentId, activePlayers.size(), tournament.getTournamentStatus());

        for (final Iterator<TournamentPlayer> iterator = activePlayers.iterator(); iterator.hasNext();) {
            final TournamentPlayer tournamentPlayer = iterator.next();
            BigDecimal playerChips = playerBalances.get(tournamentPlayer.getAccountId());
            LOG.debug("Evaluating player {} {}  with chips {}", tournamentPlayer.getPlayerId(), tournamentPlayer.getStatus(), playerChips);
            if (playerChips == null) {
                playerChips = BigDecimal.ZERO;
            }
            if (lastStatus != tournamentPlayer.getStatus() || playerChips.compareTo(lastPoints) != 0) {
                lastPoints = playerChips;
                lastStatus = tournamentPlayer.getStatus();
                leaderboardPosition = index;
            }

            tournamentPlayer.setLeaderboardPosition(leaderboardPosition);

            if (tournament.getTournamentStatus() == TournamentStatus.FINISHED) {
                tournamentPlayer.terminate(tournamentHost, tournamentId, tournament.getTournamentVariationTemplate().getGameType(),
                        tournament.getPlayers().size());
            }

            if (tournamentPlayer.getStatus().isAccountRequired()) {
                tournamentPlayer.setStack(playerChips);
            } else {
                tournamentPlayer.setStack(null);
            }

            if (TournamentPlayerStatus.ACTIVE != tournamentPlayer.getStatus()) {
                // inactive players will maintain the same position and therefore we need not revisit them
                iterator.remove();
            }

            tournamentPlayer.sendNewsIfRequired(tournamentHost, tournament);

            ++index;
        }
    }

    private void logCurrentPlayerStatus(final Tournament tournament) {
        final StringBuilder playerString = new StringBuilder();
        if (activePlayers != null) {
            for (final TournamentPlayer player : activePlayers) {
                playerString.append("[").append(player.getPlayerId())
                        .append(":").append(player.getStatus()).append("]");
            }
        }
        LOG.debug("Updated leaderboard for tournament {} with active players [{}]", tournament.getTournamentId(), playerString);
    }

    private boolean isInvalidStatus(final TournamentStatus tournamentStatus) {
        switch (tournamentStatus) {
            case RUNNING:
            case ON_BREAK:
            case FINISHED:
            case WAITING_FOR_CLIENTS:
                return false;

            default:
                return true;
        }
    }

    void updatePlayersAndBalances(final TournamentHost tournamentHost,
                                  final TournamentPlayers tournamentPlayers) {
        final Collection<TournamentPlayer> allPlayers = tournamentPlayers.getByStatus(TournamentPlayerStatus.ACTIVE);
        final Set<BigDecimal> accountIds = newHashSet(
                Collections2.transform(allPlayers, PlayerConverter.PLAYER_TO_ACCOUNT_ID));

        activePlayers = new ArrayList<>(allPlayers);
        if (activePlayers.isEmpty()) {
            LOG.debug("No players found for tournament {}", tournamentId);
            return;
        }

        try {
            LOG.debug("Getting balances for accountIDs {}", accountIds);
            playerBalances = new ConcurrentHashMap<>(
                    tournamentHost.getInternalWalletService().getBalances(accountIds));
        } catch (WalletServiceException e) {
            LOG.error("Error Getting balances for accountIDs {}", accountIds, e);
            throw new RuntimeException(e);
        }
    }

    private void eliminatePlayersInactiveAtTables(final Tournament tournament,
                                                  final TournamentHost tournamentHost) {
        final List<BigDecimal> tables = tournament.getTables();
        final Set<PlayerAtTableInformation> activePlayersForTables
                = tournamentHost.getTableService().getActivePlayers(tables);
        final int openTableCount = tournamentHost.getTableService().getOpenTableCount(tables);
        final boolean allTablesOpen = openTableCount == tables.size();
        LOG.debug("Are all tables open? {} (used tables: {}, open tables: {})",
                allTablesOpen, tables.size(), openTableCount);
        if (allTablesOpen) {
            LOG.debug("Eliminating players no longer active at tables. Active at table are {}", activePlayersForTables);
            for (final TournamentPlayer player : this.activePlayers) {
                final PlayerAtTableInformation playerAtTable = Iterables.tryFind(activePlayersForTables,
                        new PlayerIdPredicate(player.getPlayerId())).orNull();
                if (playerAtTable == null) {
                    final TournamentPlayer.EliminationReason reason;
                    final BigDecimal playerBalance = player.getStack();
                    final TournamentVariationRound currentRound = tournament.retrieveCurrentRound();
                    if (playerBalance != null && currentRound != null
                            && playerBalance.compareTo(currentRound.getMinimumBalance()) >= 0) {
                        reason = TournamentPlayer.EliminationReason.KICKED_OUT_BY_GAME;
                    } else {
                        reason = TournamentPlayer.EliminationReason.NO_CHIPS;
                    }
                    LOG.debug("Eliminating player no longer active on table: {}, reason: {}", player.getPlayerId(), reason);
                    player.eliminated(tournamentHost, tournamentId, reason,
                            tournament.getTournamentVariationTemplate().getGameType(), tournament.getPlayers().size());
                } else {
                    player.setProperties(playerAtTable.getProperties());
                }
            }
        }
    }

    public boolean isInsufficientPlayersPresent() {
        final int activePlayerCount = getActivePlayerCount();
        final boolean finished = activePlayerCount < MINIMUM_ACTIVE_PLAYERS;
        if (finished && LOG.isDebugEnabled()) {
            final String totalPlayers;
            if (activePlayers != null) {
                totalPlayers = Integer.toString(activePlayers.size());
            } else {
                totalPlayers = "null";
            }
            LOG.debug("Tournament {} can be finished (active player insufficient, active {}, total {})",
                    tournamentId, activePlayerCount, totalPlayers);
        }
        return finished;
    }

    public int getActivePlayerCount() {
        return Collections2.filter(activePlayers, PlayerFilter.ACTIVE_PLAYERS).size();
    }

    public Set<TournamentPlayer> getActivePlayers() {
        final HashSet<TournamentPlayer> result = new HashSet<>(activePlayers);
        return newHashSet(Collections2.filter(result, PlayerFilter.ACTIVE_PLAYERS));
    }

    void setActivePlayers(final List<TournamentPlayer> activePlayers) {
        this.activePlayers = activePlayers;
    }

    private void eliminateUnderMinimumBalance(final Tournament tournament, final TournamentHost tournamentHost) {
        final TournamentVariationRound round;
        round = tournament.retrieveNextRound();

        LOG.debug("Verifying player balances for tournament {} and round {}", tournamentId, round);
        if (round.getMinimumBalance() == null
                || round.getMinimumBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        for (final TournamentPlayer player : activePlayers) {
            final BigDecimal currentBalance = getBalance(player);
            if (currentBalance == null || currentBalance.compareTo(round.getMinimumBalance()) < 0) {
                LOG.debug("Eliminating player below minimum balance {}", player.getPlayerId());
                player.eliminated(tournamentHost, tournamentId, TournamentPlayer.EliminationReason.NOT_ENOUGH_CHIPS_FOR_ROUND,
                        tournament.getTournamentVariationTemplate().getGameType(), tournament.getPlayers().size());
            }
        }
    }

    private void eliminatePlayersThatAreNotOnline(final TournamentHost tournamentHost,
                                                  final Tournament tournament) {
        notNull(tournamentHost, "Tournament Host may not be null");
        LOG.debug("Verifying players are online on tournament {}", tournamentId);

        if (activePlayers != null) {
            for (final TournamentPlayer player : activePlayers) {
                if (player.getStatus() != TournamentPlayerStatus.ELIMINATED
                        && !tournamentHost.isPlayerOnline(player.getPlayerId())) {
                    LOG.debug("Eliminating offline player {}", player.getPlayerId());
                    player.eliminated(tournamentHost, tournamentId, TournamentPlayer.EliminationReason.OFFLINE,
                            tournament.getTournamentVariationTemplate().getGameType(), tournament.getPlayers().size());
                }
            }
        }
    }

    private BigDecimal getBalance(final TournamentPlayer player) {
        return playerBalances.get(player.getAccountId());
    }

    private class LeaderboardComparator implements Comparator<TournamentPlayer>, Serializable {
        private static final long serialVersionUID = -8025236919846031722L;

        public int compare(final TournamentPlayer p1, final TournamentPlayer p2) {
            if (p1.getEliminationTimestamp() == null && p2.getEliminationTimestamp() != null) {
                return -1;
            }
            if (p1.getEliminationTimestamp() != null && p2.getEliminationTimestamp() == null) {
                return 1;
            }
            if (p1.getEliminationTimestamp() != null && p2.getEliminationTimestamp() != null) {
                final int eliminationComparison = p2.getEliminationTimestamp().compareTo(p1.getEliminationTimestamp());
                if (eliminationComparison != 0) {
                    return eliminationComparison;
                }
            }

            //both are active or were eliminated at same time
            return ObjectUtils.compare(playerBalances.get(p2.getAccountId()), playerBalances.get(p1.getAccountId()));
        }
    }


    private static final class PlayerFilter implements Predicate<TournamentPlayer> {

        private static final PlayerFilter ACTIVE_PLAYERS = new PlayerFilter(TournamentPlayerStatus.ACTIVE);

        private final TournamentPlayerStatus status;

        private PlayerFilter(final TournamentPlayerStatus status) {
            this.status = status;
        }

        @Override
        @SuppressWarnings("SimplifiableIfStatement")
        public boolean apply(final TournamentPlayer input) {
            if (input == null) {
                return false;
            }
            return input.getStatus() == status;
        }
    }

    private static class PlayerConverter implements Function<TournamentPlayer, BigDecimal> {
        private static final PlayerConverter PLAYER_TO_ACCOUNT_ID = new PlayerConverter();

        @Override
        public BigDecimal apply(final TournamentPlayer input) {
            if (input == null) {
                return null;
            }

            return input.getAccountId();
        }
    }

    private static class PlayerIdPredicate implements Predicate<PlayerAtTableInformation> {
        private final BigDecimal playerId;

        public PlayerIdPredicate(final BigDecimal playerId) {
            this.playerId = playerId;
        }

        @Override
        public boolean apply(final PlayerAtTableInformation playerAtTableInformation) {
            return ObjectUtils.equals(playerAtTableInformation.getPlayer().getId(), playerId);
        }
    }
}
