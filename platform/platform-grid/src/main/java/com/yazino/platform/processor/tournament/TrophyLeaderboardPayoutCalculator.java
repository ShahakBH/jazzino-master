package com.yazino.platform.processor.tournament;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.audit.AuditLabelFactory;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPlayers;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public class TrophyLeaderboardPayoutCalculator implements Serializable {
    private static final long serialVersionUID = -6406074399615652209L;

    private static final Logger LOG = LoggerFactory.getLogger(TrophyLeaderboardPayoutCalculator.class);

    private static final String PAYOUT_TRANSACTION_TYPE = "Trophy Leaderboard Award";

    public void payout(final BigDecimal trophyLeaderboardId,
                       final TrophyLeaderboardPlayers players,
                       final Map<Integer, TrophyLeaderboardPosition> positionData,
                       final InternalWalletService internalWalletService,
                       final PlayerRepository playerRepository,
                       final AuditLabelFactory auditor) throws WalletServiceException {
        LOG.debug("Leaderboard {}: Paying out players on positions {}",
                trophyLeaderboardId, players.getPositions());
        final String auditLabel = auditor.newLabel();

        for (Integer positionNumber : players.getPositions()) {
            final TrophyLeaderboardPosition position = positionData.get(positionNumber);
            if (position == null) {
                LOG.debug("Leaderboard {}: Position {} is not defined. Stopping..",
                        trophyLeaderboardId, positionNumber);
                break;
            }

            LOG.debug("Leaderboard {}: Calculating payout for position {} (payout={})",
                    trophyLeaderboardId, positionNumber, position.getAwardPayout());

            if (position.getAwardPayout() > 0) {
                final Set<TrophyLeaderboardPlayer> playersOnPosition
                        = players.getPlayersOnPosition(position.getPosition());
                LOG.debug("Leaderboard {}: Paying {} to players {} at position {}",
                        trophyLeaderboardId, position.getAwardPayout(), playersOnPosition, positionNumber);
                final BigDecimal payout = calculatePlayersPayout(position, playersOnPosition, positionData);
                for (TrophyLeaderboardPlayer leaderboardPlayer : playersOnPosition) {
                    if (payout.compareTo(BigDecimal.ZERO) > 0) {
                        final BigDecimal playerAccountId = accountIdFor(leaderboardPlayer.getPlayerId(), playerRepository);
                        LOG.debug("Leaderboard {}: Paying {} to player {} at position {}",
                                trophyLeaderboardId, payout, leaderboardPlayer.getPlayerId(),
                                leaderboardPlayer.getLeaderboardPosition());
                        payoutTransactionForPosition(internalWalletService, auditLabel, trophyLeaderboardId,
                                playerAccountId, payout, leaderboardPlayer.getLeaderboardPosition());
                        leaderboardPlayer.setFinalPayout(payout);
                    }
                }
            }
        }
    }

    private BigDecimal accountIdFor(final BigDecimal playerId,
                                    final PlayerRepository playerRepository) {
        final Player player = playerRepository.findById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Invalid player: " + playerId);
        }
        return player.getAccountId();
    }

    private void payoutTransactionForPosition(final InternalWalletService internalWalletService,
                                              final String auditLabel,
                                              final BigDecimal trophyLeaderboardId,
                                              final BigDecimal playerAccountId,
                                              final BigDecimal payout,
                                              final int position) throws WalletServiceException {
        LOG.debug("Leaderboard {}: Performing leaderboard payout transfers with audit label {}"
                + " to account {} for amount {}", trophyLeaderboardId, auditLabel, playerAccountId, payout);
        final String txDescription = "Payout for position " + position + " from leaderboard " + trophyLeaderboardId;
        internalWalletService.postTransaction(playerAccountId,
                payout, PAYOUT_TRANSACTION_TYPE, txDescription, TransactionContext.EMPTY);
    }

    private BigDecimal calculatePlayersPayout(final TrophyLeaderboardPosition position,
                                              final Set<TrophyLeaderboardPlayer> playersOnPosition,
                                              final Map<Integer, TrophyLeaderboardPosition> positionData) {
        final BigDecimal numberOfPlayers = BigDecimal.valueOf(playersOnPosition.size());
        final BigDecimal positionPayout = retrieveTotalPayout(position.getPosition(),
                playersOnPosition.size(), positionData);
        return positionPayout.divide(numberOfPlayers, 0, BigDecimal.ROUND_DOWN);
    }

    private BigDecimal retrieveTotalPayout(final int position,
                                           final int numberOfPlayers,
                                           final Map<Integer, TrophyLeaderboardPosition> positionData) {
        BigDecimal result = BigDecimal.ZERO;
        for (int i = position; i < position + numberOfPlayers; i++) {
            final TrophyLeaderboardPosition leaderboardPosition = positionData.get(i);
            if (leaderboardPosition != null) {
                result = result.add(BigDecimal.valueOf(leaderboardPosition.getAwardPayout()));
            }
        }
        LOG.debug("Total payout for position {} ({} player(s)) = {}",
                position, numberOfPlayers, result);
        return result;
    }


}
