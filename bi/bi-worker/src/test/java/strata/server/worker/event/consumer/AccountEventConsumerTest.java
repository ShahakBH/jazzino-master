package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.AccountEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresAccountDWDAO;

import java.math.BigDecimal;

import static com.google.common.collect.Lists.newArrayList;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccountEventConsumerTest {

    @Mock
    private PostgresAccountDWDAO pgDao;
    @Mock
    private YazinoConfiguration configuration;

    private AccountEventConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new AccountEventConsumer(configuration, pgDao);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void daoIsCommitAware() {
        assertThat(underTest instanceof CommitAware, is(true));
    }

    @Test
    public void shouldSaveUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);
        final AccountEvent message = aMessageForAccount(BigDecimal.ONE);

        underTest.handle(message);
        underTest.consumerCommitting();

        verify(pgDao).saveAll(newArrayList(message));
    }

    @Test
    public void shouldSaveMultipleMessagesOnCommitUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessageForAccount(valueOf(1)));
        underTest.handle(aMessageForAccount(valueOf(2)));
        underTest.handle(aMessageForAccount(valueOf(3)));
        underTest.consumerCommitting();

        verify(pgDao).saveAll(newArrayList(aMessageForAccount(valueOf(3)), aMessageForAccount(valueOf(2)), aMessageForAccount(valueOf(1))));
    }

    @Test
    public void shouldFlushMessageOnCommitUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aMessageForAccount(valueOf(1)));
        underTest.handle(aMessageForAccount(valueOf(2)));
        underTest.handle(aMessageForAccount(valueOf(3)));
        underTest.consumerCommitting();

        verify(pgDao).saveAll(newArrayList(aMessageForAccount(valueOf(3)), aMessageForAccount(valueOf(2)), aMessageForAccount(valueOf(1))));

        underTest.handle(aMessageForAccount(valueOf(4)));
        underTest.handle(aMessageForAccount(valueOf(5)));
        underTest.consumerCommitting();

        verify(pgDao).saveAll(newArrayList(aMessageForAccount(valueOf(5)),aMessageForAccount(valueOf(4))));
    }

    @Test
    public void shouldDisableSaveUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);
        final AccountEvent message = aMessageForAccount(BigDecimal.ONE);

        underTest.handle(message);
        underTest.consumerCommitting();

        verifyZeroInteractions(pgDao);
    }

    private AccountEvent aMessageForAccount(final BigDecimal accountId) {
        return new AccountEvent(accountId, BigDecimal.TEN);
    }
}
