package com.yazino.platform.audit;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.yazino.platform.audit.message.*;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class QueuePublishingAuditService implements AuditService {
    private static final Logger LOG = LoggerFactory.getLogger(QueuePublishingAuditService.class);

    private final QueuePublishingService<TransactionProcessedMessage> transactionProcessedMessageQueuePublishingService;
    private final QueuePublishingService<CommandAuditMessage> commandAuditMessageQueuePublishingService;
    private final QueuePublishingService<GameAuditMessage> gameAuditMessageQueuePublishingService;
    private final QueuePublishingService<ExternalTransactionMessage> externalTransactionMessageQueuePublishingService;
    private final QueuePublishingService<SessionKeyMessage> sessionKeyMessageQueuePublishingService;

    private final Meter commandMeter;
    private final Meter gameMeter;
    private final Meter transactionMeter;
    private final Meter externalTransactionMeter;
    private final Meter sessionMeter;

    @Autowired
    public QueuePublishingAuditService(
            final MetricRegistry metrics,
            final QueuePublishingService<TransactionProcessedMessage> transactionProcessedMessageQueuePublishingService,
            final QueuePublishingService<CommandAuditMessage> commandAuditMessageQueuePublishingService,
            final QueuePublishingService<GameAuditMessage> gameAuditMessageQueuePublishingService,
            final QueuePublishingService<ExternalTransactionMessage> externalTransactionMessageQueuePublishingService,
            final QueuePublishingService<SessionKeyMessage> sessionKeyMessageQueuePublishingService) {
        notNull(metrics, "metrics may not be null");
        notNull(transactionProcessedMessageQueuePublishingService, "transactionProcessedMessageQueuePublishingService may not be null");
        notNull(commandAuditMessageQueuePublishingService, "commandAuditMessageQueuePublishingService may not be null");
        notNull(gameAuditMessageQueuePublishingService, "gameAuditMessageQueuePublishingService may not be null");
        notNull(externalTransactionMessageQueuePublishingService, "externalTransactionMessageQueuePublishingService may not be null");
        notNull(sessionKeyMessageQueuePublishingService, "sessionKeyMessageQueuePublishingService may not be null");

        this.commandAuditMessageQueuePublishingService = commandAuditMessageQueuePublishingService;
        this.gameAuditMessageQueuePublishingService = gameAuditMessageQueuePublishingService;
        this.externalTransactionMessageQueuePublishingService = externalTransactionMessageQueuePublishingService;
        this.sessionKeyMessageQueuePublishingService = sessionKeyMessageQueuePublishingService;
        this.transactionProcessedMessageQueuePublishingService = transactionProcessedMessageQueuePublishingService;

        this.commandMeter = metrics.meter(name(AuditService.class, "commands"));
        this.gameMeter = metrics.meter(name(AuditService.class, "games"));
        this.transactionMeter = metrics.meter(name(AuditService.class, "transactions"));
        this.externalTransactionMeter = metrics.meter(name(AuditService.class, "externalTransactions"));
        this.sessionMeter = metrics.meter(name(AuditService.class, "sessions"));
    }

    @Override
    public void transactionsProcessed(final List<Transaction> transactions) {
        notNull(transactions, "transactions is null");
        notEmpty(transactions, "transactions is empty");

        LOG.debug("Publishing transactions: {}", transactions);

        transactionProcessedMessageQueuePublishingService.send(new TransactionProcessedMessage(transactions));
        transactionMeter.mark(transactions.size());
    }

    @Override
    public void auditCommands(final List<CommandAudit> commands) {
        notEmpty(commands, "commands may not be null/empty");

        LOG.debug("Publishing command audits: {}", commands);

        commandAuditMessageQueuePublishingService.send(new CommandAuditMessage(commands));
        commandMeter.mark(commands.size());
    }

    @Override
    public void auditGame(final GameAudit gameAudit) {
        notNull(gameAudit, "gameAudit may not be null");

        LOG.debug("Publishing game audit: {}", gameAudit);

        gameAuditMessageQueuePublishingService.send(new GameAuditMessage(gameAudit));
        gameMeter.mark();
    }

    @Override
    public void externalTransactionProcessed(final ExternalTransaction externalTransaction) {
        notNull(externalTransaction, "externalTransaction may not be null");

        LOG.debug("Publishing external transaction: {}", externalTransaction);

        externalTransactionMessageQueuePublishingService.send(new ExternalTransactionMessage(externalTransaction));
        externalTransactionMeter.mark();
    }

    @Override
    public void auditSessionKey(final SessionKey sessionKey) {
        notNull(sessionKey, "sessionKey may not be null");

        LOG.debug("Publishing session key: {}", sessionKey);

        sessionKeyMessageQueuePublishingService.send(new SessionKeyMessage(sessionKey));
        sessionMeter.mark();
    }
}
