package com.yazino.platform.messaging.consumer;

import com.yazino.platform.audit.message.AuditMessageType;
import com.yazino.platform.event.message.EventMessageType;
import com.yazino.platform.messaging.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.UnsupportedEncodingException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SpringAMQPMessageListenerTest {

    private SpringAMQPMessageListener underTest;

    @Mock
    private QueueMessageConsumer consumer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new SpringAMQPMessageListener(consumer);
    }

    @Test
    public void aNullDeliveryIsIgnored() {
        underTest.handleMessage((Message) null);
        verify(consumer, never()).handle(any(Message.class));
    }

    @Test
    public void ifNotConvertedThenTheMessageIsIgnored() throws UnsupportedEncodingException {
        underTest.handleMessage(new byte[10]);
        verify(consumer, never()).handle(any(Message.class));
    }

    @Test
    public void ifNoMessageTypeIsPresentThenTheMessageIsIgnored() throws UnsupportedEncodingException {
        underTest.handleMessage(new UntypedMessage());
        verify(consumer, never()).handle(any(Message.class));
    }

    @Test
    public void messageIsRoutedToCorrectHandler() {
        final Message<EventMessageType> message = messageOfType(MessageType.TEST);
        underTest.handleMessage(message);
        verify(consumer, times(1)).handle(any(Message.class));
    }

    private enum MessageType {
        TEST
    }

    private Message messageOfType(final MessageType messageType) {
        return new Message<MessageType>() {
            private static final long serialVersionUID = -4375672450998174573L;

            @Override
            public int getVersion() {
                return 0;
            }

            @Override
            public MessageType getMessageType() {
                return messageType;
            }
        };
    }

    private static class UntypedMessage implements Message {
        private static final long serialVersionUID = -6343040453006280997L;

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public AuditMessageType getMessageType() {
            return null;
        }
    }
}
