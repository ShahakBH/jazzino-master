package com.yazino.platform.repository.chat;

import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspacesChatRepositoryIntegrationTest {
    @Autowired
    private GigaspacesChatRepository underTest;
    @Autowired
    private GigaSpace gigaSpace;

    private ChatParticipant participant;
    private ChatChannel chatChannel;
    private ChatChannelAggregate aggregate;

    @Before
    public void init() {
        participant = new ChatParticipant(BigDecimal.ONE, "channel id");
        chatChannel = new ChatChannel("channel id");
        aggregate = new ChatChannelAggregate(chatChannel, participant);
        gigaSpace.takeIfExists(participant);
        gigaSpace.takeIfExists(chatChannel);
    }

    @Test
    public void testSaveChatParticipant() {
        underTest.save(participant);
        ChatParticipant foundparticipant = gigaSpace.read(new ChatParticipant(participant.getPlayerId(), participant.getChannelId()));
        assertEquals(participant, foundparticipant);
    }

    @Test
    public void testSaveChatChannel() {
        underTest.save(chatChannel);
        ChatChannel found = gigaSpace.read(new ChatChannel(chatChannel.getChannelId()));
        assertEquals(chatChannel, found);
    }

    @Test
    public void testReadAggregate() {
        gigaSpace.write(participant);
        gigaSpace.write(chatChannel);

        ChatChannelAggregate result = underTest.readAggregate(chatChannel.getChannelId());
        assertEquals(result, aggregate);
    }

    @Test
    public void testReadParticipants() {
        gigaSpace.write(participant);
        ChatParticipant[] result = underTest.readParticipantsForChannel(chatChannel.getChannelId());
        assertArrayEquals(new ChatParticipant[]{participant}, result);
    }

    @Test
    public void testRemoveChatParticipant() {
        gigaSpace.write(participant);
        underTest.remove(participant);
        ChatParticipant found = gigaSpace.read(new ChatParticipant(participant.getPlayerId(), participant.getChannelId()));
        assertNull(found);
    }

    @Test
    public void testRequest() {
        GigaspaceChatRequest request = new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL, participant.getPlayerId(), chatChannel.getChannelId(), null);
        underTest.request(request);
        GigaspaceChatRequest found = gigaSpace.read(new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL));
        assertEquals(request, found);
    }

    @Test
    public void testReadChatParticipant() {
        gigaSpace.write(participant);
        ChatParticipant found = underTest.read(participant);
        assertEquals(participant, found);
    }

    @Test
    public void testReadChatChannel() {
        gigaSpace.write(chatChannel);
        ChatChannel found = underTest.dirtyRead(new ChatChannel(chatChannel.getChannelId()));
        assertEquals(chatChannel, found);
    }

    @Test
    public void testGetOrCreateForLocationWhereExists() {
        String locationId = "location id";
        ChatChannel chatChannelForLocation = new ChatChannel(chatChannel.getChannelId());
        chatChannelForLocation.setChannelType(ChatChannelType.table);
        chatChannelForLocation.setLocationId(locationId);
        gigaSpace.write(chatChannelForLocation);
        ChatChannel found = underTest.getOrCreateForLocation(locationId);
        assertEquals(chatChannelForLocation, found);
    }

    @Test
    @Transactional
    public void lockReturnsTournamentFromSpace() {
        gigaSpace.write(chatChannel, 3000);
        final ChatChannel found = underTest.lock(chatChannel.getChannelId());
        Assert.assertNotNull(found);
        Assert.assertEquals(chatChannel, found);
    }

    @Transactional
    @Test(expected = ConcurrentModificationException.class)
    public void lockWillThrowExceptionIfTournamentDoesNotExists() {
        underTest.lock(chatChannel.getChannelId());
    }
}
