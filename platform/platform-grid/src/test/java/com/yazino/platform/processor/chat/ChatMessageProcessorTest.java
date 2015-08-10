package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ChatMessageProcessorTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;

    @Mock
    private ChatMessageWorker chatMessageWorker;

    private ChatMessageProcessor underTest;

    private GigaspaceChatRequest request;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        underTest = new ChatMessageProcessor(chatMessageWorker);

        final Map<ChatRequestArgument, String> args = new HashMap<ChatRequestArgument, String>();
        args.put(ChatRequestArgument.MESSAGE, "hello");
        request = new GigaspaceChatRequest(ChatRequestType.SEND_MESSAGE, PLAYER_ID, "aChannelId", "aLocationId", args);
    }

    @Test
    public void testProcessRequest() {
        underTest.processRequest(request);

        verify(chatMessageWorker).sendChatMessage(request);
    }
}
