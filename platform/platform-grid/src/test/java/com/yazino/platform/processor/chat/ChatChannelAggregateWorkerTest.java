package com.yazino.platform.processor.chat;

import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.HostDocumentPublisher;
import com.yazino.platform.model.chat.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.*;

public class ChatChannelAggregateWorkerTest {
    @Mock
    private HostDocumentPublisher hostDocumentPublisher;
    @Mock
    private DestinationFactory destinationFactory;
    @Mock
    private Destination destination;

    private ChatChannelAggregate aggregate;
    private ChatChannelAggregateWorker unit;

    @SuppressWarnings({"unchecked"})
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        final ChatChannel channel = new ChatChannel(ChatChannelType.personal, "location Id");
        channel.setChannelId("channel id");
        final ChatParticipant participant1 = new ChatParticipant(BigDecimal.ONE, channel.getChannelId(), "nickname 1");
        final ChatParticipant participant2 = new ChatParticipant(BigDecimal.TEN, channel.getChannelId(), "nickname 2");
        aggregate = new ChatChannelAggregate(channel, participant1, participant2);

        unit = new ChatChannelAggregateWorker(hostDocumentPublisher, destinationFactory);

        when(destinationFactory.players(anySet())).thenReturn(destination);
    }

    @Test
    public void documentIsDispatchedToAllParticipants() {
        final BigDecimal senderId = BigDecimal.ONE;

        unit.dispatchToAllParticipants(aggregate, senderId);

        verify(hostDocumentPublisher).publish(asList((HostDocument) any(ChatChannelStatusHostDocument.class)));
        verify(destinationFactory).players(set(BigDecimal.ONE, BigDecimal.TEN));
    }

    @Test
    public void documentIsDispatchedToAllParticipantsAndSenderWhenSenderIsNotAParticipant() {
        final BigDecimal senderId = new BigDecimal(100);

        unit.dispatchToAllParticipants(aggregate, senderId);

        verify(hostDocumentPublisher).publish(asList((HostDocument) any(ChatChannelStatusHostDocument.class)));
        verify(destinationFactory).players(set(BigDecimal.ONE, BigDecimal.TEN, senderId));
    }

    private <T> Set<T> set(final T... objs) {
        final Set<T> set = new HashSet<T>();
        if (objs != null) {
            set.addAll(asList(objs));
        }
        return set;
    }
}
