package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import com.yazino.platform.repository.chat.ChatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AddParticipantProcessorTest {
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatChannelWorker chatChannelWorker;

    private AddParticipantProcessor underTest;
    private GigaspaceChatRequest request;
    private final String channelId = "chanel id";
    private final BigDecimal playerId = BigDecimal.ONE;
    private final String locationId = "location id";
    private final String nickname = "on eyed joe";
    private final Map<ChatRequestArgument, String> args = new HashMap<ChatRequestArgument, String>() {{
        put(ChatRequestArgument.NICKNAME, nickname);
        put(ChatRequestArgument.PLAYER_ID, playerId.toString());
    }};

    @Before
    public void init() {
        underTest = new AddParticipantProcessor(chatRepository, chatChannelWorker);
        request = mock(GigaspaceChatRequest.class);
    }

    @Test
    public void testProcessRequest() {
        when(request.isValid()).thenReturn(true);
        when(request.getChannelId()).thenReturn(channelId);
        when(request.getLocationId()).thenReturn(locationId);
        when(request.getPlayerId()).thenReturn(playerId);
        when(request.getArgs()).thenReturn(args);
        final ChatChannel channel = mock(ChatChannel.class);
        when(chatRepository.dirtyRead(new ChatChannel(channelId))).thenReturn(channel);
        when(chatRepository.lock(channelId)).thenReturn(channel);
        when(chatChannelWorker.canAddParticipantsToChannel(chatRepository, channelId, playerId, locationId)).thenReturn(true);

        underTest.processRequest(request);

        verify(chatRepository).save(channel);
    }
}
