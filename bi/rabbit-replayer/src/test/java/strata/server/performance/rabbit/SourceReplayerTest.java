package strata.server.performance.rabbit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link SourceReplayer} class.
 */
public class SourceReplayerTest {

    private final String exchange = "test-exchange";
    private final long interval = 3590L;
    private final String routingKey = "TEST_PLAYER.123";
    private final byte[] body = "FooBar Test".getBytes();
    private final Sleeper sleeper = mock(Sleeper.class);
    private final ReplayableMessage message = mock(ReplayableMessage.class);
    private final AmqpTemplate template = mock(AmqpTemplate.class);
    private final ReplayableSource source = mock(ReplayableSource.class);

    private final SourceReplayer replayer = new SourceReplayer(template, source);

    @Before
    public void setup() {
        when(source.getExchange()).thenReturn(exchange);
        when(source.getMessageInterval()).thenReturn(interval);
        when(message.getRoutingKey()).thenReturn(routingKey);
        when(message.getBody()).thenReturn(body);
        replayer.setSleeper(sleeper);
        when(source.next()).thenReturn(message);
    }

    @Test
    public void shouldAllowInitializationBeforeAttemptingToIterate() throws Exception {
        replayer.run();
        verify(source).init();
    }

    @Test
    public void shouldPublishMessageToCorrectExchange() throws Exception {
        when(source.hasNext()).thenReturn(true, false);
        replayer.run();
        verify(template).send(eq(exchange), any(String.class), any(Message.class));
    }

    @Test
    public void shouldPublishMessageUsingCorrectRoutingKey() throws Exception {
        when(source.hasNext()).thenReturn(true, false);
        replayer.run();
        verify(template).send(eq(exchange), any(String.class), any(Message.class));
    }

    @Test
    public void shouldPublishMessageWithCorrectContent() throws Exception {
        when(source.hasNext()).thenReturn(true, false);
        replayer.run();
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        verify(template).send(eq(exchange), eq(routingKey), captor.capture());
        Message message = captor.getValue();
        assertEquals(new String(body), new String(message.getBody()));
    }

    @Test
    public void shouldPublishMessagesAtCorrectInterval() throws Exception {
        when(source.hasNext()).thenReturn(true, false);
        replayer.run();
        verify(sleeper).sleep(interval);
    }

    @Test
    public void shouldFinishWhenNoMoreMessages() throws Exception {
        when(source.hasNext()).thenReturn(true, true, false);
        replayer.run();

        verify(template, times(2)).send(eq(exchange), eq(routingKey), any(Message.class));
    }

    @Test
    public void shouldFinishWhenChannelGetsClosed() throws Exception {
        doNothing().doNothing().doNothing().doThrow(new AmqpException("Closed")).when(template).send(eq(exchange), any(String.class), any(Message.class));
        when(source.hasNext()).thenReturn(true);
        replayer.run();
        verify(template, times(4)).send(eq(exchange), eq(routingKey), any(Message.class));
    }

    @Test
    public void shouldAllowDestructionAfterFinishedRunning() throws Exception {
        when(source.hasNext()).thenReturn(true, false);
        replayer.run();
        verify(source).destroy();
    }
}
