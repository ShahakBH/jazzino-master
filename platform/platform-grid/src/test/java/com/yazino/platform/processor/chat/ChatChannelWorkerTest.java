package com.yazino.platform.processor.chat;

import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelType;
import com.yazino.platform.model.chat.ChatParticipant;
import com.yazino.platform.repository.chat.ChatRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChatChannelWorkerTest {
    ChatRepository chatRepository;
    String channelId = "chanel id";
    String locationId = "location id";
    BigDecimal playerId = BigDecimal.TEN;
    ChatParticipant participant;
    ChatChannelWorker underTest;
    ChatChannel chatChannel;

    @Before
    public void init() {
        underTest = new ChatChannelWorker();
        chatRepository = mock(ChatRepository.class);
        participant = new ChatParticipant(playerId, channelId);
        chatChannel = new ChatChannel(channelId);
    }

    private void canSendMessagesToChannelSetup(boolean canSend) {
        when(chatRepository.read(participant)).thenReturn((canSend ? new ChatParticipant(playerId, channelId) : null));
    }

    private void readChannelSetup(boolean exists, ChatChannelType channelType, String locationId) {
        when(chatRepository.dirtyRead(chatChannel)).thenReturn((exists ? new ChatChannel(channelType, locationId) : null));
    }

    @Test
    public void testCanSendMessagesToChannel() {
        canSendMessagesToChannelSetup(true);

        boolean result = underTest.canSendMessagesToChannel(chatRepository, channelId, playerId);

        assertTrue(result);
    }

    @Test
    public void testCannotSendMessagesToChannel() {
        canSendMessagesToChannelSetup(false);

        boolean result = underTest.canSendMessagesToChannel(chatRepository, channelId, playerId);

        assertFalse(result);
    }

    @Test
    public void testCanAddParticipantsToPersonalChannel() {
        canSendMessagesToChannelSetup(true);
        readChannelSetup(true, ChatChannelType.personal, null);

        boolean result = underTest.canAddParticipantsToChannel(chatRepository, channelId, playerId, null);

        assertTrue(result);
    }

    @Test
    public void testCanAddParticipantsToTableChannel() {
        readChannelSetup(true, ChatChannelType.table, locationId);

        boolean result = underTest.canAddParticipantsToChannel(chatRepository, channelId, playerId, locationId);

        assertTrue(result);
    }

    @Test
    public void testCannnotAddParticipantsToChannelThatDoesNotExist() {
        readChannelSetup(false, ChatChannelType.personal, null);

        boolean result = underTest.canAddParticipantsToChannel(chatRepository, channelId, playerId, null);

        assertFalse(result);
    }

    @Test
    public void testCannnotAddParticipantsToTableChannel() {
        canSendMessagesToChannelSetup(true);
        readChannelSetup(true, ChatChannelType.table, locationId);

        boolean result = underTest.canAddParticipantsToChannel(chatRepository, channelId, playerId, null);

        assertFalse(result);
    }
}
