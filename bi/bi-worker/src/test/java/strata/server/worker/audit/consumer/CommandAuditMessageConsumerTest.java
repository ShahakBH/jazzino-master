package strata.server.worker.audit.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.CommandAudit;
import com.yazino.platform.audit.message.CommandAuditMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.audit.persistence.PostgresCommandAuditDAO;

import java.math.BigDecimal;
import java.util.Date;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CommandAuditMessageConsumerTest {
    @Mock
    private YazinoConfiguration configuration;
    @Mock
    private PostgresCommandAuditDAO postgresCommandAuditDao;

    private CommandAuditMessageConsumer underTest;

    @Before
    public void setUp() {
        underTest = new CommandAuditMessageConsumer(configuration, postgresCommandAuditDao);
    }

    @Test
    public void aMessageIsSavedToThePostgresqlDAOWhenRedshiftIsEnabled() {
        final CommandAuditMessage message = new CommandAuditMessage(asList(aCommandAudit("1"), aCommandAudit("2")));
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(message);
        underTest.consumerCommitting();

        verify(postgresCommandAuditDao).saveAll(asList(aCommandAudit("1"), aCommandAudit("2")));
    }

    @Test
    public void multipleMessagesAreSavedToThePostgresqlDAOOnCommit() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(new CommandAuditMessage(asList(aCommandAudit("1"), aCommandAudit("2"))));
        underTest.handle(new CommandAuditMessage(asList(aCommandAudit("3"), aCommandAudit("4"))));
        underTest.consumerCommitting();

        verify(postgresCommandAuditDao).saveAll(asList(aCommandAudit("1"), aCommandAudit("2"), aCommandAudit("3"), aCommandAudit("4")));
    }

    @Test
    public void thePostgresqlDAOMessageBatchIsFlushedOnCommit() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(new CommandAuditMessage(asList(aCommandAudit("1"), aCommandAudit("2"))));
        underTest.consumerCommitting();
        underTest.handle(new CommandAuditMessage(asList(aCommandAudit("3"), aCommandAudit("4"))));
        underTest.consumerCommitting();

        verify(postgresCommandAuditDao).saveAll(asList(aCommandAudit("1"), aCommandAudit("2")));
        verify(postgresCommandAuditDao).saveAll(asList(aCommandAudit("3"), aCommandAudit("4")));
    }

    @Test
    public void aMessageIsNotSavedToThePostgresqlDAOWhenRedshiftIsDisabled() {
        final CommandAuditMessage message = new CommandAuditMessage(asList(aCommandAudit("1"), aCommandAudit("2")));

        underTest.handle(message);
        underTest.consumerCommitting();

        verifyZeroInteractions(postgresCommandAuditDao);
    }

    private CommandAudit aCommandAudit(final String uuid) {
        return new CommandAudit("anAuditLabelFor" + uuid, "aHostname", new Date(1000000), BigDecimal.valueOf(100),
                200L, "aCommandType", new String[]{"arg1", "arg2"}, BigDecimal.valueOf(100), uuid);
    }
}
