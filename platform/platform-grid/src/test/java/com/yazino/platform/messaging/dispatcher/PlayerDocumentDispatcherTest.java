package com.yazino.platform.messaging.dispatcher;

import com.yazino.platform.messaging.publisher.SpringAMQPRoutedTemplates;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class PlayerDocumentDispatcherTest {

    private AmqpTemplate template;
    private PlayerDocumentDispatcher underTest;
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;

    @Before
    public void setUp() {
        final SpringAMQPRoutedTemplates templates = mock(SpringAMQPRoutedTemplates.class);
        template = mock(AmqpTemplate.class);
        when(templates.hostFor(PLAYER_ID)).thenReturn("host1");
        when(templates.templateFor("host1")).thenReturn(template);
        underTest = new PlayerDocumentDispatcher(templates);
    }

    @Test
    public void shouldDispatchDocument() throws IOException {
        underTest.dispatch(PLAYER_ID, "docType", "docBody");
        final Message message = afterMessageIsPublished();
        assertArrayEquals("docBody".getBytes("UTF-8"), message.getBody());
        assertEquals("docType", message.getMessageProperties().getContentType());
    }

    private Message afterMessageIsPublished() throws IOException {
        final ArgumentCaptor<Message> publisherCaptor = ArgumentCaptor.forClass(Message.class);
        verify(template).send(eq("PLAYER." + PLAYER_ID), publisherCaptor.capture());
        return publisherCaptor.getValue();
    }

}
