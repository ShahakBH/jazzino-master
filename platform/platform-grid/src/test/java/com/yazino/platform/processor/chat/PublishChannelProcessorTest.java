package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelAggregate;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import com.yazino.platform.repository.chat.ChatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PublishChannelProcessorTest {
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatChannelAggregateWorker chatChannelAggregateWorker;

    private GigaspaceChatRequest request;
    private ChatChannelAggregate aggregate;
    private final String channelId = "channel id";
    private final BigDecimal playerId = BigDecimal.ONE;

    private PublishChannelProcessor underTest;

    @Before
    public void init() {
        underTest = new PublishChannelProcessor(chatRepository, chatChannelAggregateWorker);
        final String locationId = "location id";
        request = new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL, playerId, channelId, locationId);
        aggregate = mock(ChatChannelAggregate.class);
    }

    @Test
    public void testProcessRequest() {
        final ChatChannel channel = mock(ChatChannel.class);
        when(chatRepository.dirtyRead(new ChatChannel(channelId))).thenReturn(channel);
        when(chatRepository.lock(channelId)).thenReturn(channel);
        when(chatRepository.readAggregate(channelId)).thenReturn(aggregate);

        underTest.processRequest(request);

        verify(chatChannelAggregateWorker).dispatchToAllParticipants(aggregate, playerId);
        verify(chatRepository).save(channel);
    }
}
