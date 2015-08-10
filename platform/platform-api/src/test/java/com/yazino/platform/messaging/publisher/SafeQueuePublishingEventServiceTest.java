package com.yazino.platform.messaging.publisher;

import com.yazino.platform.messaging.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
public class SafeQueuePublishingEventServiceTest {
    
    private SafeQueuePublishingEventService underTest;
    private SpringAMQPRoutedQueuePublishingService publisher;

    @Before
    public void setup() {
        publisher = mock(SpringAMQPRoutedQueuePublishingService.class);
        underTest = new SafeQueuePublishingEventService(publisher);
    }

    @Test
    public void shouldInvokeDelegateToSendMessage() {
        Message mockMessage = mock(Message.class);
        underTest.send(mockMessage);
        verify(publisher).send(mockMessage);
    }
    
    @Test
    public void shouldNotThrowExceptionIfMessageSendFails() {
        Message mockMessage = mock(Message.class);
        Mockito.doThrow(new ClassCastException()).when(publisher).send(mockMessage);
        underTest.send(mockMessage);
    }
}
