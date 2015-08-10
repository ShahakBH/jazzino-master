package com.yazino.platform.processor.chat;

import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelAggregate;
import com.yazino.platform.model.chat.ChatChannelType;
import com.yazino.platform.model.chat.ChatParticipant;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class ChatChannelAggregateTest {
    @Test
    public void testConstructor() {
        ChatChannel channel = new ChatChannel(ChatChannelType.personal, "location Id");
        channel.setChannelId("channel id");
        ChatParticipant participant1 = new ChatParticipant(BigDecimal.ONE, channel.getChannelId(), "nickname");
        ChatParticipant participant2 = new ChatParticipant(BigDecimal.TEN, channel.getChannelId(), "nickname 2");


        ChatChannelAggregate underTest = new ChatChannelAggregate(channel, participant1, participant2);
        assertEquals(channel.getChannelId(), underTest.getId());
        assertEquals(channel.getChannelType(), underTest.getType());
        assertEquals(channel.getChannelType().canAddParticipants(), underTest.canAddParticipants());
        assertEquals(channel.getLocationId(), underTest.getLocationId());
        assertEquals(2, underTest.getChatParticipants().length);
        assertEquals(participant1.getPlayerId(), underTest.getChatParticipants()[0].getPlayerId());
        assertEquals(participant2.getPlayerId(), underTest.getChatParticipants()[1].getPlayerId());
        assertEquals(participant1.getNickname(), underTest.getChatParticipants()[0].getNickname());
        assertEquals(participant2.getNickname(), underTest.getChatParticipants()[1].getNickname());
    }


}
