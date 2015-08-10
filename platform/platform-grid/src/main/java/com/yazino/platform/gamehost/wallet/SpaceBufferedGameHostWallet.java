package com.yazino.platform.gamehost.wallet;

import com.yazino.platform.account.GameHostWallet;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.PostTransactionAtTable;
import com.yazino.platform.model.table.TableTransactionRequest;
import com.yazino.platform.table.PlayerInformation;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.TransactionType;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.apache.commons.lang3.Validate.notNull;

public class SpaceBufferedGameHostWallet implements BufferedGameHostWallet {
    private static final Logger LOG = LoggerFactory.getLogger(SpaceBufferedGameHostWallet.class);

    private List<PostTransactionAtTable> pendingTransactions = new ArrayList<>();
    private Set<BigDecimal> pendingReleases = new HashSet<>();

    private final GameHostWallet delegate;
    private final GigaSpace tableGigaSpace;
    private final BigDecimal tableId;
    private final String auditLabel;

    SpaceBufferedGameHostWallet(final GigaSpace tableGigaSpace,
                                final GameHostWallet delegate,
                                final BigDecimal tableId,
                                final String auditLabel) {
        notNull(tableGigaSpace, "tableGigaSpace may not be null");
        notNull(delegate, "delegate may not be null");
        notNull(tableId, "tableId may not be null");

        this.auditLabel = auditLabel;
        this.tableId = tableId;
        this.tableGigaSpace = tableGigaSpace;
        this.delegate = delegate;
    }

    @Override
    public void flush() {
        if (pendingTransactions.isEmpty() && pendingReleases.isEmpty()) {
            return;
        }

        final TableTransactionRequest ttw = new TableTransactionRequest(
                tableId, pendingTransactions, pendingReleases, auditLabel);
        ttw.setTimestamp(new Date());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending TableTransactionRequest: " + ReflectionToStringBuilder.reflectionToString(ttw));
        }

        tableGigaSpace.write(ttw);

        pendingTransactions = new ArrayList<>();
        pendingReleases = new HashSet<>();
    }

    @Override
    public void post(final BigDecimal toTableId,
                     final Long gameId,
                     final PlayerInformation playerInformation,
                     final BigDecimal amount,
                     final TransactionType transactionType,
                     final String txAuditLabel,
                     final String reference,
                     final String txReplyLabel)
            throws WalletServiceException {
        final TransactionContext transactionContext = transactionContext()
                .withGameId(gameId)
                .withTableId(tableId)
                .withSessionId(playerInformation.getSessionId())
                .withPlayerId(playerInformation.getPlayerId())
                .build();
        final PostTransactionAtTable post = new PostTransactionAtTable(playerInformation.getPlayerId(),
                playerInformation.getAccountId(), amount, transactionType.toString(), reference, txReplyLabel, transactionContext);
        pendingTransactions.add(post);
    }

    @Override
    public BigDecimal getBalance(final BigDecimal accountId)
            throws WalletServiceException {
        return delegate.getBalance(accountId);
    }

    @Override
    public int numberOfPendingTransactions() {
        return pendingTransactions.size();
    }
}
