package com.yazino.platform.gamehost.wallet;

import com.yazino.game.api.*;
import com.yazino.platform.account.GameHostWallet;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.util.UUIDSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TableBoundGamePlayerWalletFactory implements GamePlayerWalletFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TableBoundGamePlayerWalletFactory.class);

    private final Table table;
    private final GameHostWallet gameHostWallet;
    private final String auditLabel;
    private final UUIDSource uidSource;

    public TableBoundGamePlayerWalletFactory(final Table table,
                                             final GameHostWallet gameHostWallet,
                                             final String auditLabel,
                                             final UUIDSource uidSource) {
        notNull(table, "Table may not be null");
        notNull(gameHostWallet, "wallet service may not be null");
        notNull(auditLabel, "auditLabel may not be null");
        notNull(uidSource, "uidSource may not be null");

        this.auditLabel = auditLabel;
        this.table = table;
        this.gameHostWallet = gameHostWallet;
        this.uidSource = uidSource;
    }

    @Override
    public GamePlayerWallet forPlayer(final GamePlayer gamePlayer) {
        notNull(gamePlayer, "Game Player may not be null");

        return new TableBoundGamePlayerWallet(gamePlayer.getId());
    }

    public Table getTable() {
        return table;
    }

    public String getAuditLabel() {
        return auditLabel;
    }

    public GameHostWallet getGameHostWallet() {
        return gameHostWallet;
    }

    private class TableBoundGamePlayerWallet implements GamePlayerWallet {
        private final BigDecimal playerId;

        public TableBoundGamePlayerWallet(final BigDecimal playerId) {
            notNull(playerId, "Player ID may not be null");

            this.playerId = playerId;
        }

        @Override
        public String increaseBalanceBy(final BigDecimal amount,
                                        final String txAuditLabel,
                                        final String reference)
                throws GameException {
            final String replyLabel = uidSource.getNewUUID();

            final PlayerInformation playerInformation = getPlayerInformation();

            if (!TableBoundGamePlayerWalletFactory.this.auditLabel.equals(txAuditLabel)) {
                LOG.warn(String.format("AuditLabels don't match [%s] vs [%s]",
                        TableBoundGamePlayerWalletFactory.this.auditLabel, txAuditLabel));
            }

            try {
                gameHostWallet.post(table.getTableId(), table.getGameId(), playerInformation, amount,
                        TransactionType.Return, TableBoundGamePlayerWalletFactory.this.auditLabel,
                        reference, replyLabel);

            } catch (WalletServiceException e) {
                LOG.warn(String.format("Transfer failed for player %s on table %s with account %s",
                        playerId, table.getTableId(), playerInformation.getAccountId()));
                throw new GameException("Unable to reserve %s from account %s",
                        amount, playerInformation.getAccountId(), true);
            }

            return replyLabel;
        }

        @Override
        public String decreaseBalanceBy(final BigDecimal amount,
                                        final String txAuditLabel,
                                        final String reference)
                throws GameException {
            final String replyLabel = uidSource.getNewUUID();

            final PlayerInformation playerInformation = getPlayerInformation();
            try {
                gameHostWallet.post(table.getTableId(), table.getGameId(), playerInformation,
                        BigDecimal.ZERO.subtract(amount), TransactionType.Stake,
                        TableBoundGamePlayerWalletFactory.this.auditLabel, reference, replyLabel);

            } catch (WalletServiceException e) {
                LOG.warn(String.format("Reservation failed for player %s on table %s with account %s",
                        playerId, table.getTableId(), playerInformation.getAccountId()));
                throw new GameException("Unable to reserve %s from account %s",
                        amount, playerInformation.getAccountId(), true);
            }

            return replyLabel;
        }

        @Override
        public BigDecimal getBalance() throws GameException {
            // TODO TX-RETURN: this should return the cached balance once update on tx-return is implemented
            try {
                return gameHostWallet.getBalance(getPlayerInformation().getAccountId());
            } catch (WalletServiceException e) {
                throw new GameException("Could not get balance", e);
            }
        }

        private PlayerInformation getPlayerInformation() {
            final PlayerInformation playerInformation = table.playerAtTable(playerId);
            if (playerInformation == null) {
                throw new IllegalStateException(String.format("Player %s is not at table %s",
                        playerId, table.getTableId()));
            }
            return playerInformation;
        }
    }
}
