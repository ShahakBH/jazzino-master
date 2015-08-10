package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatParticipant;
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
public class LeaveChannelProcessorTest {
    @Mock
    private ChatRepository chatRepository;

    private LeaveChannelProcessor underTest;
    private GigaspaceChatRequest request;

    final String channelId = "channel id";
    final BigDecimal playerId = BigDecimal.ONE;
    final String locationId = "location id";

    @Before
    public void init() {
        underTest = new LeaveChannelProcessor(chatRepository);
        request = new GigaspaceChatRequest(ChatRequestType.LEAVE_CHANNEL, playerId, channelId, locationId);
    }

    @Test
    public void testProcessRequest() {
        final ChatChannel channel = mock(ChatChannel.class);
        when(chatRepository.dirtyRead(new ChatChannel(channelId))).thenReturn(channel);
        when(chatRepository.lock(channelId)).thenReturn(channel);

        underTest.processRequest(request);

        verify(chatRepository).remove(new ChatParticipant(playerId, channelId));
        verify(chatRepository).request(new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL, playerId, channelId, locationId));
        verify(chatRepository).save(channel);
    }

}
