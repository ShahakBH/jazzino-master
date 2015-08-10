package com.yazino.platform.processor.session;

import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.NewsEventHostDocument;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.model.session.InboxMessageReceived;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InboxMessageReceivedProcessorTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final NewsEvent NEWS_EVENT = new NewsEvent.Builder(
            PLAYER_ID, new ParameterisedMessage("message")).setType(NewsEventType.NEWS).setImage("image").build();
    private static final DateTime DEFAULT_TIME = new DateTime();

    @Mock
    private PlayerSessionRepository playerSessionRepository;
    @Mock
    private InboxMessageRepository inboxMessageRepository;
    @Mock
    private HostDocumentDispatcher hostDocumentDispatcher;
    @Mock
    private DestinationFactory destinationFactory;
    @Mock
    private Destination destination;

    private InboxMessageReceived event;
    private InboxMessageReceivedProcessor underTest;

    @Before
    public void setUp() throws Exception {
        when(destinationFactory.player(PLAYER_ID)).thenReturn(destination);

        final InboxMessage unreadMessage = new InboxMessage(PLAYER_ID, NEWS_EVENT, DEFAULT_TIME);
        event = new InboxMessageReceived(unreadMessage);
        underTest = new InboxMessageReceivedProcessor(playerSessionRepository, inboxMessageRepository,
                hostDocumentDispatcher, destinationFactory);
    }

    @Test
    public void shouldSendNewsMessageIfPlayerIsOnline() {
        when(playerSessionRepository.isOnline(PLAYER_ID)).thenReturn(true);

        underTest.processRequest(event);

        verify(hostDocumentDispatcher).send(new NewsEventHostDocument(NEWS_EVENT,
                destinationFactory.player(PLAYER_ID)));
    }

    @Test
    public void shouldMarkMessageAsRedIfPlayerIsOnline() {
        final InboxMessage readMessage = new InboxMessage(PLAYER_ID, NEWS_EVENT, DEFAULT_TIME);
        readMessage.setRead(true);
        when(playerSessionRepository.isOnline(PLAYER_ID)).thenReturn(true);

        underTest.processRequest(event);

        verify(inboxMessageRepository).save(readMessage);
    }

    @Test
    public void shouldDiscardEmptyEvent() {
        underTest.processRequest(new InboxMessageReceived());
        verifyNoMoreInteractions(playerSessionRepository);
        verifyNoMoreInteractions(hostDocumentDispatcher);
    }
}
