package strata.server.worker.audit.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.ExternalTransaction;
import com.yazino.platform.audit.message.ExternalTransactionMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.audit.persistence.PostgresExternalTransactionDAO;

import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExternalTransactionMessageConsumerTest {
    @Mock
    private PostgresExternalTransactionDAO externalDao;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private ExternalTransactionMessageConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new ExternalTransactionMessageConsumer(yazinoConfiguration, externalDao);
    }

    @Test(expected = IllegalStateException.class)
    public void handleCannotBeCalledWhenClassWasCreatedWithTheCGLibConstructor() {
        new ExternalTransactionMessageConsumer().handle(anExternalTransactionMessage(1));
    }

    @Test
    public void shouldSaveUsingExternalDaoWhenRedshiftPropertyIsTrue() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(anExternalTransactionMessage(1));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList((ExternalTransaction) anExternalTransactionMessage(1)));
    }

    @Test
    public void shouldNotSaveUsingExternalDaoWhenRedshiftPropertyIsFalse() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);

        underTest.handle(anExternalTransactionMessage(1));
        underTest.consumerCommitting();

        verifyZeroInteractions(externalDao);
    }

    @Test
    public void shouldSaveMultipleItemsUsingExternalDaoWhenRedshiftPropertyIsTrue() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(anExternalTransactionMessage(1));
        underTest.handle(anExternalTransactionMessage(2));
        underTest.handle(anExternalTransactionMessage(3));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList((ExternalTransaction) anExternalTransactionMessage(1),
                anExternalTransactionMessage(2), anExternalTransactionMessage(3)));
    }

    @Test
    public void shouldFlushTheExternalBatchAfterCommit() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(anExternalTransactionMessage(1));
        underTest.handle(anExternalTransactionMessage(2));
        underTest.consumerCommitting();
        underTest.handle(anExternalTransactionMessage(3));
        underTest.handle(anExternalTransactionMessage(4));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList((ExternalTransaction) anExternalTransactionMessage(1), anExternalTransactionMessage(2)));
        verify(externalDao).saveAll(asList((ExternalTransaction) anExternalTransactionMessage(3), anExternalTransactionMessage(4)));
    }

    private ExternalTransactionMessage anExternalTransactionMessage(final int accountId) {
        final ExternalTransactionMessage message = new ExternalTransactionMessage();
        message.setAccountId(BigDecimal.valueOf(accountId));
        return message;
    }
}
