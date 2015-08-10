package com.yazino.platform.service.session;

import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.repository.session.InboxMessageRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.Mockito.*;

public class GigaspaceRemotingInboxServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final ParameterisedMessage MESSAGE = new ParameterisedMessage("aMessage");
    private static final NewsEvent NEWS_EVENT = new NewsEvent.Builder(PLAYER_ID, MESSAGE).setType(NewsEventType.NEWS).setImage("someImage").build();

    private TimeSource timeSource = new SettableTimeSource(0);
    private InboxMessageRepository inboxMessageRepository = mock(InboxMessageRepository.class);
    private InboxMessage message;

    private GigaspaceRemotingInboxService underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new GigaspaceRemotingInboxService(inboxMessageRepository);
        message = new InboxMessage(PLAYER_ID, NEWS_EVENT, new DateTime(timeSource.getCurrentTimeStamp()));
    }

    @Test
    public void shouldSendUnreadMessagesToPlayerIfRequested() {
        when(inboxMessageRepository.findUnreadMessages(PLAYER_ID)).thenReturn(Arrays.asList(message));

        underTest.checkNewMessages(PLAYER_ID);

        verify(inboxMessageRepository).messageReceived(message);
    }
}
