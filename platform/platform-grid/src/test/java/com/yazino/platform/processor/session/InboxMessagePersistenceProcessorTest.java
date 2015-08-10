package com.yazino.platform.processor.session;

import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.model.session.InboxMessagePersistenceRequest;
import com.yazino.platform.persistence.session.InboxMessageDAO;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;


public class InboxMessagePersistenceProcessorTest {
    private final InboxMessageDAO inboxMessageDAO = mock(InboxMessageDAO.class);
    private static final InboxMessage MESSAGE = new InboxMessage(BigDecimal.TEN, new NewsEvent.Builder(BigDecimal.TEN, new ParameterisedMessage("message")).setType(NewsEventType.NEWS).setImage("image").build(), new DateTime());
    private InboxMessagePersistenceProcessor persistenceProcessor;

    @Before
    public void setUp() throws Exception {
        persistenceProcessor = new InboxMessagePersistenceProcessor(inboxMessageDAO);
    }

    @Test
    public void shouldUserDaoToPersistMessage() {
        InboxMessagePersistenceRequest request = persistenceProcessor.processRequest(new InboxMessagePersistenceRequest(MESSAGE));
        verify(inboxMessageDAO).save(MESSAGE);
        assertNull(request);
    }

    @Test
    public void shouldIgnoreEmptyRequest() {
        InboxMessagePersistenceRequest request = persistenceProcessor.processRequest(new InboxMessagePersistenceRequest());
        verifyNoMoreInteractions(inboxMessageDAO);
        assertNull(request);
    }

    @Test
    public void marksRequestIfErrorInDao() {
        Mockito.doThrow(new RuntimeException()).when(inboxMessageDAO).save(any(InboxMessage.class));
        InboxMessagePersistenceRequest request = persistenceProcessor.processRequest(new InboxMessagePersistenceRequest(MESSAGE));
        assertEquals(InboxMessagePersistenceRequest.STATUS_ERROR, request.getStatus());
    }
}
