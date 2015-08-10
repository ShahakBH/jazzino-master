package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.messaging.dispatcher.MemoryDocumentDispatcher;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelType;
import com.yazino.platform.model.chat.ChatParticipant;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.test.DummyProfanityService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ChatIntegrationTest {
    @Autowired
    private GigaSpace gigaSpace;

    @Autowired
    private DummyProfanityService dummyProfanityService;

    @Autowired
    @Qualifier("documentDispatcher")
    private MemoryDocumentDispatcher documentDispatcher;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private AddParticipantProcessor addParticipantProcessor;

    @Autowired
    private ChatMessageProcessor chatMessageProcessor;

    @Autowired
    private PublishChannelProcessor publishChannelProcessor;

    @Before
    @After
    public void clearSpace() {
        gigaSpace.clear(null);
        documentDispatcher.clear();
    }

    @Test
    @Transactional
    public void testAddParticipant() throws InterruptedException {
        ChatChannel channel = new ChatChannel(ChatChannelType.personal);
        chatRepository.save(channel);
        BigDecimal player1 = BigDecimal.ONE;
        BigDecimal player2 = BigDecimal.TEN;

        ChatParticipant participant1 = new ChatParticipant(player1, channel.getChannelId(), "player 1");
        chatRepository.save(participant1);
        GigaspaceChatRequest request = new GigaspaceChatRequest(ChatRequestType.ADD_PARTICIPANT, player1, channel.getChannelId(), null);
        request.getArgs().put(ChatRequestArgument.PLAYER_ID, player2.toString());
        request.getArgs().put(ChatRequestArgument.NICKNAME, "player 2");

        chatRepository.request(request);

        GigaspaceChatRequest creq = gigaSpace.readIfExists(addParticipantProcessor.getTemplate());
        assertNotNull(creq);
        addParticipantProcessor.processRequest(creq);

        ChatParticipant[] participants = chatRepository.readParticipantsForChannel(channel.getChannelId());
        assertEquals(2, participants.length);
        GigaspaceChatRequest pubrequest = gigaSpace.readIfExists(publishChannelProcessor.getTemplate());
        assertNotNull(pubrequest);
    }

    @Test
    @Transactional
    public void testPublishChannel() throws InterruptedException {
        ChatChannel channel = new ChatChannel(ChatChannelType.personal);
        chatRepository.save(channel);
        BigDecimal player1 = BigDecimal.ONE;
        BigDecimal player2 = BigDecimal.TEN;

        ChatParticipant participant1 = new ChatParticipant(player1, channel.getChannelId(), "player 1");
        chatRepository.save(participant1);
        ChatParticipant participant2 = new ChatParticipant(player2, channel.getChannelId(), "player 2");
        chatRepository.save(participant2);

        GigaspaceChatRequest request = new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL, player1, channel.getChannelId(), null);
        chatRepository.request(request);

        GigaspaceChatRequest pubrequest = gigaSpace.read(publishChannelProcessor.getTemplate());
        publishChannelProcessor.processRequest(pubrequest);
        assertEquals(2, documentDispatcher.getSentCount());

    }

    @Test
    public void testSendMessage() {
        ChatChannel channel = new ChatChannel(ChatChannelType.personal);
        chatRepository.save(channel);
        ChatParticipant p1 = new ChatParticipant(BigDecimal.ONE, channel.getChannelId(), "p1");
        chatRepository.save(p1);
        ChatParticipant p10 = new ChatParticipant(BigDecimal.TEN, channel.getChannelId(), "p10");
        chatRepository.save(p10);
        dummyProfanityService.getBadWords().add("xstream");
        GigaspaceChatRequest request = new GigaspaceChatRequest(ChatRequestType.SEND_MESSAGE, p1.getPlayerId(), channel.getChannelId(), null);
        request.getArgs().put(ChatRequestArgument.MESSAGE, "foo xstream");

        chatRepository.request(request);
        GigaspaceChatRequest gigaRequest = gigaSpace.read(chatMessageProcessor.getTemplate());
        assertNotNull(gigaRequest);
        chatMessageProcessor.processRequest(gigaRequest);

        assertNotNull(documentDispatcher.getLastDocument());
        assertFalse(documentDispatcher.getLastDocument().getBody().contains("xstream"));
        assertTrue(documentDispatcher.getLastDocument().getBody().contains("*******"));
    }
}
