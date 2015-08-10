package strata.server.worker.event.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.InvitationEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresInvitationDWDAO;

import java.math.BigDecimal;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InvitationEventConsumerTest {
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private PostgresInvitationDWDAO externalDao;

    private InvitationEventConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new InvitationEventConsumer(yazinoConfiguration, externalDao);
    }

    @Test
    public void shouldSaveUsingRedshiftWhenEnabled() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(anInvitationEvent(1));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(anInvitationEvent(1)));
    }

    @Test
    public void shouldSaveMultipleEventsUsingRedshiftWhenEnabled() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(anInvitationEvent(1));
        underTest.handle(anInvitationEvent(2));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(anInvitationEvent(1), anInvitationEvent(2)));
    }

    @Test
    public void shouldFlushBatchOnCommit() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(anInvitationEvent(1));
        underTest.handle(anInvitationEvent(2));
        underTest.consumerCommitting();
        underTest.handle(anInvitationEvent(3));
        underTest.handle(anInvitationEvent(4));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(anInvitationEvent(1), anInvitationEvent(2)));
        verify(externalDao).save(newHashSet(anInvitationEvent(3), anInvitationEvent(4)));
    }

    @Test
    public void shouldNotSaveUsingRedshiftWhenDisabled() {
        underTest.handle(anInvitationEvent(1));
        underTest.consumerCommitting();

        verifyZeroInteractions(externalDao);
    }

    private InvitationEvent anInvitationEvent(final int playerId) {
        final InvitationEvent invitationEvent = new InvitationEvent();
        invitationEvent.setIssuingPlayerId(BigDecimal.valueOf(playerId));
        return invitationEvent;
    }
}
