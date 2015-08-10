package com.yazino.platform.gamehost.wallet;

import com.yazino.game.api.TransactionType;
import com.yazino.platform.account.GameHostWallet;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.PostTransactionAtTable;
import com.yazino.platform.model.table.TableTransactionRequest;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.table.PlayerInformation;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class SpaceGameHostWallet implements GameHostWallet {
    private static final Logger LOG = LoggerFactory.getLogger(SpaceGameHostWallet.class);

    private final GigaSpace gigaSpace;
    private final InternalWalletService internalWalletService;

    @Autowired
    public SpaceGameHostWallet(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                               final InternalWalletService internalWalletService) {
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(internalWalletService, "internalWalletService may not be null");

        this.gigaSpace = gigaSpace;
        this.internalWalletService = internalWalletService;
    }

    @Override
    public void post(final BigDecimal tableId,
                     final Long gameId,
                     final PlayerInformation playerInformation,
                     final BigDecimal amount,
                     final TransactionType transactionType,
                     final String auditLabel,
                     final String reference,
                     final String replyLabel) {
        final TransactionContext transactionContext = transactionContext()
                .withGameId(gameId)
                .withTableId(tableId)
                .withSessionId(playerInformation.getSessionId())
                .withPlayerId(playerInformation.getPlayerId())
                .build();
        final PostTransactionAtTable post = new PostTransactionAtTable(playerInformation.getPlayerId(),
                playerInformation.getAccountId(), amount, transactionType.toString(), reference, replyLabel, transactionContext);
        final TableTransactionRequest ttw = new TableTransactionRequest(
                tableId, Arrays.asList(post), Collections.<BigDecimal>emptySet(), auditLabel);
        ttw.setTimestamp(new Date());

        LOG.debug("Sending TableTransactionRequest: {}", ttw);

        gigaSpace.write(ttw);
    }

    @Override
    public BigDecimal getBalance(final BigDecimal accountId)
            throws WalletServiceException {
        return internalWalletService.getBalance(accountId);
    }
}
