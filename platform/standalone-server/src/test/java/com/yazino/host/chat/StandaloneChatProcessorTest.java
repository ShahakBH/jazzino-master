package com.yazino.host.chat;

import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StandaloneChatProcessorTest {

    private StandaloneChatProcessor underTest;

    @Before
    public void setUp() {
        underTest = new StandaloneChatProcessor(mock(ChatRepository.class),
                mock(DocumentDispatcher.class),
                mock(PlayerRepository.class),
                mock(PlayerSessionRepository.class),
                mock(ChatRequestSource.class)
        );
    }

    @Test
    public void shouldRouteLeaveRequest() {
        final ChatRequestProcessor processor = mock(ChatRequestProcessor.class);
        setProcessor(processor, ChatRequestType.LEAVE_CHANNEL);
        final GigaspaceChatRequest request = new GigaspaceChatRequest(ChatRequestType.LEAVE_CHANNEL);
        underTest.process(request);
        verify(processor).processRequest(request);
    }

    @Test
    public void shouldRouteChatMessageRequest() {
        final ChatRequestProcessor processor = mock(ChatRequestProcessor.class);
        setProcessor(processor, ChatRequestType.SEND_MESSAGE);
        final GigaspaceChatRequest request = new GigaspaceChatRequest(ChatRequestType.SEND_MESSAGE);
        underTest.process(request);
        verify(processor).processRequest(request);
    }

    @SuppressWarnings({"unchecked"})
    private void setProcessor(final Object processor, ChatRequestType requestType) {
        final Object field = ReflectionTestUtils.getField(underTest, "processors");
        ((HashMap<ChatRequestType, Object>) field).put(requestType, processor);
    }

    public interface ChatRequestProcessor {
        void processRequest(final GigaspaceChatRequest request);
    }
}
