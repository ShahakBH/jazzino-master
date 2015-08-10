package strata.server.worker.audit.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.Transaction;
import com.yazino.platform.audit.message.TransactionProcessedMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.audit.persistence.PostgresTransactionLogDAO;
import strata.server.worker.audit.playedtracking.PlayerPlayedTracking;

import java.math.BigDecimal;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransactionProcessedMessageConsumerTest {
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private PostgresTransactionLogDAO externalDao;
    @Mock
    private PlayerPlayedTracking playerPlayedTracking;

    private TransactionProcessedMessageConsumer underTest;

    @Before
    public void setUp() {
        underTest = new TransactionProcessedMessageConsumer(yazinoConfiguration, externalDao, playerPlayedTracking);
    }

    @Test
    public void shouldSaveUsingExternalDaoIfRedshiftPropertyIsTrue() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aTransactionProcessedMessage());
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList(aTransaction(1), aTransaction(2)));
    }

    @Test
    public void shouldSaveMultipleTransactionMessagesUsingExternalDaoIfRedshiftPropertyIsTrue() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessageWith(aTransaction(1), aTransaction(2)));
        underTest.handle(aMessageWith(aTransaction(3), aTransaction(4)));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList(aTransaction(1), aTransaction(2), aTransaction(3), aTransaction(4)));
    }


    @Test
    public void shouldFlushExternalBatchOnCommit() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessageWith(aTransaction(1), aTransaction(2)));
        underTest.consumerCommitting();
        underTest.handle(aMessageWith(aTransaction(3), aTransaction(4)));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList(aTransaction(1), aTransaction(2)));
        verify(externalDao).saveAll(asList(aTransaction(3), aTransaction(4)));
    }

    @Test
    public void shouldNotSaveUsingExternalDaoIfRedshiftPropertyIsFalse() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);

        underTest.handle(aTransactionProcessedMessage());
        underTest.consumerCommitting();

        verifyZeroInteractions(externalDao);
    }

    @Test
    public void shouldTrackPlayers() {
        underTest.handle(aTransactionProcessedMessage());
        underTest.consumerCommitting();

        verify(playerPlayedTracking).track(aTransactionProcessedMessage().getTransactions());
    }

    @Test
    public void shouldIgnoreExceptionsFromTracking() {
        doThrow(new RuntimeException()).when(playerPlayedTracking).track(any(Collection.class));

        underTest.handle(aTransactionProcessedMessage());
        underTest.consumerCommitting();
    }

    private TransactionProcessedMessage aTransactionProcessedMessage() {
        return aMessageWith(aTransaction(1), aTransaction(2));
    }
    private TransactionProcessedMessage aMessageWith(final Transaction... transactions) {
        final TransactionProcessedMessage message = new TransactionProcessedMessage();
        message.setTransactions(asList(transactions));
        return message;
    }

    private Transaction aTransaction(final int accountId) {
        final Transaction transaction = new Transaction();
        transaction.setAccountId(BigDecimal.valueOf(accountId));
        return transaction;
    }
}
