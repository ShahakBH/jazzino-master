package com.yazino.platform.audit;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.yazino.platform.audit.message.*;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.Platform.WEB;
import static java.math.BigDecimal.TEN;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueuePublishingAuditServiceTest {
    private static final HashMap<String, Object> CLIENT_CONTEXT = new HashMap<>();

    @Mock
    private QueuePublishingService<TransactionProcessedMessage> transactionProcessedMessageQueuePublishingService;
    @Mock
    private QueuePublishingService<CommandAuditMessage> commandAuditMessageQueuePublishingService;
    @Mock
    private QueuePublishingService<GameAuditMessage> gameAuditMessageQueuePublishingService;
    @Mock
    private QueuePublishingService<ExternalTransactionMessage> externalTransactionMessageQueuePublishingService;
    @Mock
    private QueuePublishingService<SessionKeyMessage> sessionKeyMessageQueuePublishingService;
    @Mock
    private MetricRegistry metrics;
    @Mock
    private Meter commandMeter;
    @Mock
    private Meter gameMeter;
    @Mock
    private Meter transactionMeter;
    @Mock
    private Meter externalTransactionMeter;
    @Mock
    private Meter sessionMeter;

    private QueuePublishingAuditService underTest;

    @Before
    public void setUp() throws Exception {
        when(metrics.meter(name(AuditService.class, "commands"))).thenReturn(commandMeter);
        when(metrics.meter(name(AuditService.class, "games"))).thenReturn(gameMeter);
        when(metrics.meter(name(AuditService.class, "transactions"))).thenReturn(transactionMeter);
        when(metrics.meter(name(AuditService.class, "externalTransactions"))).thenReturn(externalTransactionMeter);
        when(metrics.meter(name(AuditService.class, "sessions"))).thenReturn(sessionMeter);

        underTest = new QueuePublishingAuditService(metrics,
                transactionProcessedMessageQueuePublishingService,
                commandAuditMessageQueuePublishingService,
                gameAuditMessageQueuePublishingService,
                externalTransactionMessageQueuePublishingService,
                sessionKeyMessageQueuePublishingService);
    }

    @Test
    public void transactionProcessedShouldSendMessage() {
        final List<Transaction> txs = Arrays.asList(tx(1, 100), tx(2, 200), tx(3, 300));
        underTest.transactionsProcessed(txs);
        verify(transactionProcessedMessageQueuePublishingService).send(new TransactionProcessedMessage(txs));
    }

    @Test
    public void transactionProcessedShouldIncrementTheMeter() {
        underTest.transactionsProcessed(Arrays.asList(tx(1, 100), tx(2, 200), tx(3, 300)));

        verify(transactionMeter).mark(3);
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void transactionProcessedShouldRefuseNullCollection() {
        underTest.transactionsProcessed(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transactionProcessedShouldRefuseEmptyCollection() {
        underTest.transactionsProcessed(new ArrayList<Transaction>());
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void commandAuditShouldRefuseANullCollection() {
        underTest.auditCommands(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commandAuditShouldRefuseAnEmptyCollection() {
        underTest.auditCommands(Collections.<CommandAudit>emptyList());
    }

    @Test
    public void commandAuditShouldSendAMessage() {
        underTest.auditCommands(asList(aCommand("cmd1"), aCommand("cmd2")));

        verify(commandAuditMessageQueuePublishingService).send(new CommandAuditMessage(asList(aCommand("cmd1"), aCommand("cmd2"))));
    }

    @Test
    public void commandAuditShouldIncrementTheMeter() {
        underTest.auditCommands(asList(aCommand("cmd1"), aCommand("cmd2")));

        verify(commandMeter).mark(2);
    }

    @Test(expected = NullPointerException.class)
    public void gameAuditShouldRefuseANullCollection() {
        underTest.auditGame(null);
    }

    @Test
    public void gameAuditShouldSendAMessage() {
        underTest.auditGame(aGame());

        verify(gameAuditMessageQueuePublishingService).send(new GameAuditMessage(aGame()));
    }

    @Test
    public void gameAuditShouldIncrementTheMeter() {
        underTest.auditGame(aGame());

        verify(gameMeter).mark();
    }

    @Test(expected = NullPointerException.class)
    public void externalTransactionProcessedShouldRefuseANullCollection() {
        underTest.externalTransactionProcessed(null);
    }

    @Test
    public void externalTransactionProcessedShouldSendAMessage() {
        underTest.externalTransactionProcessed(anExternalTransaction());

        verify(externalTransactionMessageQueuePublishingService).send(new ExternalTransactionMessage(anExternalTransaction()));
    }

    @Test
    public void externalTransactionProcessedShouldIncrementTheMeter() {
        underTest.externalTransactionProcessed(anExternalTransaction());

        verify(externalTransactionMeter).mark();
    }

    @Test(expected = NullPointerException.class)
    public void auditSessionKeyShouldRefuseANullSessionKey() {
        underTest.auditSessionKey(null);
    }

    @Test
    public void auditSessionKeyShouldSendAMessage() {
        underTest.auditSessionKey(aSessionKey());

        verify(sessionKeyMessageQueuePublishingService).send(new SessionKeyMessage(aSessionKey()));
    }

    @Test
    public void auditSessionKeyShouldIncrementTheMeter() {
        underTest.auditSessionKey(aSessionKey());

        verify(sessionMeter).mark();
    }

    @Test
    public void theCommandMeterShouldBeCreatedDuringInitialisation() {
        verify(metrics).meter(name(AuditService.class, "commands"));
    }

    @Test
    public void theGameMeterShouldBeCreatedDuringInitialisation() {
        verify(metrics).meter(name(AuditService.class, "games"));
    }

    @Test
    public void theTransactionMeterShouldBeCreatedDuringInitialisation() {
        verify(metrics).meter(name(AuditService.class, "transactions"));
    }

    @Test
    public void theExternalTransactionMeterShouldBeCreatedDuringInitialisation() {
        verify(metrics).meter(name(AuditService.class, "externalTransactions"));
    }

    @Test
    public void theSessionMeterShouldBeCreatedDuringInitialisation() {
        verify(metrics).meter(name(AuditService.class, "sessions"));
    }

    private SessionKey aSessionKey() {
        return new SessionKey(BigDecimal.ZERO, TEN, BigDecimal.ONE, "aSessionKey", "anIpAddress", "aReferrer", "aPartner", "aloginUrl", CLIENT_CONTEXT);
    }

    private ExternalTransaction anExternalTransaction() {
        return new ExternalTransaction(BigDecimal.valueOf(100), "anInternalTxId", "anExternalTxId", "aCCMessage",
                new Date(65456), "aCurrency", BigDecimal.valueOf(200), BigDecimal.valueOf(300), "anObscuredCCNumber",
                "aCashier", "aGameType", "anExternalTxStatus", "aTxLogType", BigDecimal.TEN, 123l, WEB, "aPaymentOptionId",
                null, null, null, null);
    }

    private GameAudit aGame() {
        return new GameAudit("anAuditLabel", "aHostName", new Date(234342), BigDecimal.valueOf(100), 200L, 300L,
                "anObservableStatusXml", "anInternalStatusXml", newHashSet(BigDecimal.valueOf(400)));
    }

    private CommandAudit aCommand(final String uuid) {
        return new CommandAudit("anAuditLabel", "aHostname", new Date(23424234), BigDecimal.valueOf(100), 200L,
                "aType", new String[]{"arg1"}, BigDecimal.valueOf(300), uuid);
    }

    private Transaction tx(final int accountId, final int amount) {
        return new Transaction(BigDecimal.valueOf(accountId), BigDecimal.valueOf(amount), "Stake", "ref1", 123L,
                BigDecimal.valueOf(4000), 123L, TEN, BigDecimal.valueOf(3141592), BigDecimal.valueOf(88348L));
    }
}
