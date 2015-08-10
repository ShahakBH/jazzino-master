package strata.server.worker.event.consumer;

import com.yazino.client.log.ClientLogEvent;
import com.yazino.client.log.ClientLogEventMessageType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresAnalyticsLogDWDAO;
import strata.server.worker.event.persistence.PostgresClientLogDWDAO;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ClientLogEventConsumerTest {
    @Mock
    private PostgresClientLogDWDAO clientLogDWDAO;
    @Mock
    private PostgresAnalyticsLogDWDAO analyticsLogDWDAO;

    private ClientLogEventConsumer underTest;

    @Before
    public void setUp() {
        underTest = new ClientLogEventConsumer(clientLogDWDAO, analyticsLogDWDAO);
    }

    @Test
    public void shouldNotSaveBeforeCommit() {
        ClientLogEvent event = new ClientLogEvent();
        underTest.handle(event);
        verifyZeroInteractions(clientLogDWDAO);
        verifyZeroInteractions(analyticsLogDWDAO);
    }

    @Test
    public void shouldSaveClientLogEventsOnConsumerCommitting() {
        ClientLogEvent event1 = new ClientLogEvent(null, "event1", ClientLogEventMessageType.LOG_EVENT);
        ClientLogEvent event2 = new ClientLogEvent(null, "event2", ClientLogEventMessageType.LOG_EVENT);

        underTest.handle(event1);
        underTest.handle(event2);
        underTest.consumerCommitting();

        verify(clientLogDWDAO).saveAll(asList(event1, event2));
        verifyNoMoreInteractions(analyticsLogDWDAO);
    }

    @Test
    public void shouldSaveAnalyticLogEventsOnConsumerCommitting() {
        ClientLogEvent analytic1 = new ClientLogEvent(null, "analytic1", ClientLogEventMessageType.LOG_ANALYTICS);
        ClientLogEvent analytic2 = new ClientLogEvent(null, "analytic1", ClientLogEventMessageType.LOG_ANALYTICS);

        underTest.handle(analytic1);
        underTest.handle(analytic2);
        underTest.consumerCommitting();

        verify(analyticsLogDWDAO).saveAll(asList(analytic1, analytic2));
        verifyNoMoreInteractions(clientLogDWDAO);
    }

    @Test
    public void shouldSaveAnalyticLogEventsAndClientLogEventsOnConsumerCommitting() {
        ClientLogEvent event1 = new ClientLogEvent(null, "event1", ClientLogEventMessageType.LOG_EVENT);
        ClientLogEvent event2 = new ClientLogEvent(null, "event2", ClientLogEventMessageType.LOG_EVENT);
        ClientLogEvent analytic1 = new ClientLogEvent(null, "analytic1", ClientLogEventMessageType.LOG_ANALYTICS);
        ClientLogEvent analytic2 = new ClientLogEvent(null, "analytic1", ClientLogEventMessageType.LOG_ANALYTICS);

        underTest.handle(event1);
        underTest.handle(event2);
        underTest.handle(analytic1);
        underTest.handle(analytic2);
        underTest.consumerCommitting();

        verify(clientLogDWDAO).saveAll(asList(event1, event2));
        verify(analyticsLogDWDAO).saveAll(asList(analytic1, analytic2));
    }

    @Test
    public void shouldHandleEmptyBatch(){
        underTest.consumerCommitting();
        verifyZeroInteractions(clientLogDWDAO);
        verifyZeroInteractions(analyticsLogDWDAO);
    }
}
