package com.yazino.platform.repository.session;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.model.session.InboxMessagePersistenceRequest;
import com.yazino.platform.model.session.InboxMessageReceived;
import com.yazino.platform.persistence.session.InboxMessageDAO;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceInboxMessageRepositoryTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;

    @Mock
    private GigaSpace localGigaSpace;
    @Mock
    private GigaSpace globalGigaSpace;
    @Mock
    private Routing routing;
    @Mock
    private InboxMessageDAO inboxMessageDAO;

    private InboxMessageRepository underTest;
    private InboxMessage message;

    @Before
    public void setUp() throws Exception {
        underTest = new GigaspaceInboxMessageRepository(localGigaSpace, globalGigaSpace, routing, inboxMessageDAO);

        message = new InboxMessage(PLAYER_ID, new NewsEvent.Builder(PLAYER_ID, new ParameterisedMessage("message")).setType(NewsEventType.NEWS).setImage("image").build(), new DateTime());
    }

    @Test
    public void sendingAMessageForALocallyRoutedPlayerShouldWriteAPersistenceRequestAndReceivedMessageToTheLocalSpace() {
        when(routing.isRoutedToCurrentPartition(PLAYER_ID)).thenReturn(true);

        underTest.send(message);

        verify(localGigaSpace).writeMultiple(new Object[]{new InboxMessagePersistenceRequest(message), new InboxMessageReceived(message)});
    }

    @Test
    public void sendingAMessageForARemotelyRoutedPlayerShouldWriteAPersistenceRequestAndReceivedMessageToTheGlobalSpace() {
        when(routing.isRoutedToCurrentPartition(PLAYER_ID)).thenReturn(false);

        underTest.send(message);

        verify(globalGigaSpace).writeMultiple(new Object[]{new InboxMessagePersistenceRequest(message), new InboxMessageReceived(message)});
    }

    @Test
    public void savingAMessageForALocallyRoutedPlayerShouldWriteAPersistenceRequestToTheLocalSpace() {
        when(routing.isRoutedToCurrentPartition(PLAYER_ID)).thenReturn(true);

        underTest.save(message);

        verify(localGigaSpace).write(new InboxMessagePersistenceRequest(message));
    }

    @Test
    public void savingAMessageForARemotelyRoutedPlayerShouldWriteAPersistenceRequestToTheGlobalSpace() {
        when(routing.isRoutedToCurrentPartition(PLAYER_ID)).thenReturn(false);

        underTest.save(message);

        verify(globalGigaSpace).write(new InboxMessagePersistenceRequest(message));
    }

    @Test
    public void receivingAMessageForALocallyRoutedPlayerShouldWriteAReceivedObjectToTheLocalSpace() {
        when(routing.isRoutedToCurrentPartition(PLAYER_ID)).thenReturn(true);

        underTest.messageReceived(message);

        verify(localGigaSpace).write(new InboxMessageReceived(message));
    }

    @Test
    public void receivingAMessageForARemotelyRoutedPlayerShouldWriteAReceivedObjectToTheGlobalSpace() {
        when(routing.isRoutedToCurrentPartition(PLAYER_ID)).thenReturn(false);

        underTest.messageReceived(message);

        verify(globalGigaSpace).write(new InboxMessageReceived(message));
    }

    @Test
    public void unreadMessageAreReadFromTheDAO() {
        final List<InboxMessage> expected = Arrays.asList(message);
        when(inboxMessageDAO.findUnreadMessages(PLAYER_ID)).thenReturn(expected);

        final List<InboxMessage> actual = underTest.findUnreadMessages(PLAYER_ID);

        assertEquals(expected, actual);
    }
}
