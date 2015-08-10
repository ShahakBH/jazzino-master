package strata.server.worker.audit.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.GameAudit;
import com.yazino.platform.audit.message.GameAuditMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.audit.persistence.PostgresGameAuditDAO;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GameAuditMessageConsumerTest {
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private PostgresGameAuditDAO externalDao;

    private GameAuditMessageConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new GameAuditMessageConsumer(yazinoConfiguration, externalDao);
    }

    @Test
    public void shouldSaveUsingExternalDaoWhenRedshiftPropertyIsTrue() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aGameAuditMessage("1"));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList((GameAudit) aGameAuditMessage("1")));
    }

    @Test
    public void shouldSaveMultipleMessagesUsingExternalDaoWhenRedshiftPropertyIsTrue() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aGameAuditMessage("1"));
        underTest.handle(aGameAuditMessage("2"));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList((GameAudit) aGameAuditMessage("1"), aGameAuditMessage("2")));
    }

    @Test
    public void shouldFlushExternalBatchUsingExternalDaoWhenCommitIsCalled() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aGameAuditMessage("1"));
        underTest.handle(aGameAuditMessage("2"));
        underTest.consumerCommitting();
        underTest.handle(aGameAuditMessage("3"));
        underTest.handle(aGameAuditMessage("4"));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(asList((GameAudit) aGameAuditMessage("1"), aGameAuditMessage("2")));
        verify(externalDao).saveAll(asList((GameAudit) aGameAuditMessage("3"), aGameAuditMessage("4")));
    }

    @Test
    public void shouldNotSaveUsingExternalDaoWhenRedshiftPropertyIsFalse() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);

        underTest.handle(aGameAuditMessage("1"));
        underTest.consumerCommitting();

        verifyZeroInteractions(externalDao);
    }

    private GameAuditMessage aGameAuditMessage(final String auditLabel) {
        final GameAuditMessage gameAuditMessage = new GameAuditMessage();
        gameAuditMessage.setAuditLabel(auditLabel);
        return gameAuditMessage;
    }
}
