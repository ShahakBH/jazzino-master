package com.yazino.platform.processor.account;

import com.yazino.platform.audit.AuditService;
import com.yazino.platform.audit.message.Transaction;
import com.yazino.platform.model.account.AccountTransaction;
import com.yazino.platform.model.account.AccountTransactionPersistenceRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.openspaces.events.polling.ReceiveHandler;
import org.openspaces.events.polling.receive.MultiTakeReceiveOperationHandler;
import org.openspaces.events.polling.receive.ReceiveOperationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 5, passArrayAsIs = true)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class AccountTransactionPersistenceProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AccountTransactionPersistenceProcessor.class);

    private static final int BATCH_SIZE = 1000;
    private static final AccountTransactionPersistenceRequest TEMPLATE
            = new AccountTransactionPersistenceRequest();

    private AuditService auditService;

    @Autowired(required = true)
    public void setAccountDAO(final AuditService newAuditService) {
        notNull(newAuditService, "newAuditService may not be null");

        this.auditService = newAuditService;
    }

    @ReceiveHandler
    public ReceiveOperationHandler receiveHandler() {
        final MultiTakeReceiveOperationHandler receiveHandler = new MultiTakeReceiveOperationHandler();
        receiveHandler.setMaxEntries(BATCH_SIZE);
        return receiveHandler;
    }

    @EventTemplate
    public AccountTransactionPersistenceRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    @Transactional
    public AccountTransactionPersistenceRequest[] processAccountPersistenceRequest(
            final AccountTransactionPersistenceRequest[] request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Attempting to save transactions " + ArrayUtils.toString(request));
        }

        AccountTransactionPersistenceRequest[] afterProcessing = null;
        try {
            final List<Transaction> transactions = new ArrayList<>();
            for (final AccountTransactionPersistenceRequest accountTransactionPersistenceRequest : request) {
                transactions.add(buildTransactionMessage(accountTransactionPersistenceRequest));
            }
            auditService.transactionsProcessed(transactions);

        } catch (Throwable t) {
            LOG.error("Exception saving transactions", t);

            afterProcessing = request;
            for (AccountTransactionPersistenceRequest accountTransactionPersistenceRequest : afterProcessing) {
                accountTransactionPersistenceRequest.setStatus(AccountTransactionPersistenceRequest.STATUS_ERROR);
            }
        }

        return afterProcessing;
    }

    private Transaction buildTransactionMessage(
            final AccountTransactionPersistenceRequest accountTransactionPersistenceRequest) {
        final AccountTransaction tx = accountTransactionPersistenceRequest.getAccountTransaction();

        return new Transaction(tx.getAccountId(), tx.getAmount(), tx.getType(),
                tx.getReference(), tx.getTimestamp(), tx.getRunningBalance(), tx.getTransactionContext().getGameId(),
                tx.getTransactionContext().getTableId(), tx.getTransactionContext().getSessionId(),
                tx.getTransactionContext().getPlayerId());
    }
}
