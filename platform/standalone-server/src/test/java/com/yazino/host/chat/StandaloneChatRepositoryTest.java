package com.yazino.host.chat;

import org.junit.Before;
import org.junit.Test;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelAggregate;
import com.yazino.platform.model.chat.ChatChannelType;
import com.yazino.platform.model.chat.ChatParticipant;

import java.util.HashSet;

import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

public class StandaloneChatRepositoryTest {

    private StandaloneChatRepository underTest;

    @Before
    public void setUp() {
        underTest = new StandaloneChatRepository();
    }

    @Test
    public void shouldSaveParticipants() {
        final ChatParticipant participant1 = new ChatParticipant(valueOf(1), "c1", "p1");
        final ChatParticipant participant2 = new ChatParticipant(valueOf(2), "c1", "p2");
        underTest.save(participant1);
        underTest.save(participant2);
        final ChatParticipant[] participants = underTest.readParticipantsForChannel("c1");
        final HashSet<ChatParticipant> expected = new HashSet<ChatParticipant>(asList(participant1, participant2));
        final HashSet<ChatParticipant> actual = new HashSet<ChatParticipant>(asList(participants));
        assertEquals(expected, actual);
        assertEquals(participant1, underTest.read(participant1));
    }

    @Test
    public void shouldSaveChannel() {
        final ChatChannel channel = new ChatChannel(ChatChannelType.table, "a");
        underTest.save(channel);
        assertEquals(channel, underTest.getOrCreateForLocation("a"));
    }

    @Test
    public void shouldSaveChannelWithoutLocation() {
        final ChatChannel channel = new ChatChannel("aChannel");
        underTest.save(channel);
        assertNotNull(underTest.readAggregate("aChannel"));
    }

    @Test
    public void shouldCreateIdsForChannels() {
        final ChatChannel channel = new ChatChannel(ChatChannelType.table, "a");
        underTest.save(channel);
        assertNotNull(channel.getChannelId());
        assertNotNull(underTest.readAggregate(channel.getChannelId()));
    }

    @Test
    public void shouldCreateChannelForLocation() {
        final ChatChannel channel = underTest.getOrCreateForLocation("location");
        assertNotNull(channel);
        assertNotNull(channel.getChannelId());
        assertEquals("location", channel.getLocationId());
    }

    @Test
    public void shouldRetrieveParticipantsForEmptyChannel() {
        assertArrayEquals(new ChatParticipant[0], underTest.readParticipantsForChannel("a channel"));
    }

    @Test
    public void shouldReadAggregate() {
        final ChatParticipant participant1 = new ChatParticipant(valueOf(1), "c1", "p1");
        final ChatParticipant participant2 = new ChatParticipant(valueOf(2), "c1", "p2");
        underTest.save(participant1);
        underTest.save(participant2);
        final ChatChannel c1 = new ChatChannel("c1");
        underTest.save(c1);
        final ChatChannelAggregate expected = new ChatChannelAggregate(c1, participant1, participant2);
        final ChatChannelAggregate actual = underTest.readAggregate("c1");
        assertEquals(expected, actual);
    }

    @Test
    public void shouldReadParticipantsForSession() {
        final ChatParticipant participant1 = new ChatParticipant(valueOf(1), "c1", "p1");
        final ChatParticipant participant2 = new ChatParticipant(valueOf(1), "c2", "p2");
        underTest.save(participant1);
        underTest.save(participant2);
        final ChatParticipant[] participants = underTest.readParticipantsForSession(valueOf(1));
        final HashSet<ChatParticipant> expected = new HashSet<ChatParticipant>(asList(participant1, participant2));
        final HashSet<ChatParticipant> actual = new HashSet<ChatParticipant>(asList(participants));
        assertEquals(expected, actual);
        assertEquals(participant1, underTest.read(participant1));
    }

    @Test
    public void shouldRemoveParticipant() {
        underTest.save(new ChatChannel("c1"));
        final ChatParticipant participant1 = new ChatParticipant(valueOf(1), "c1", "p1");
        underTest.save(participant1);
        assertEquals(1, underTest.readParticipantsForSession(valueOf(1)).length);
        assertEquals(1, underTest.readParticipantsForChannel("c1").length);
        assertEquals(1, underTest.readAggregate("c1").getChatParticipants().length);
        underTest.remove(participant1);
        assertEquals(0, underTest.readParticipantsForSession(valueOf(1)).length);
        assertEquals(0, underTest.readParticipantsForChannel("c1").length);
        assertEquals(0, underTest.readAggregate("c1").getChatParticipants().length);
    }

    @Test
    public void shouldRemoveChannel() {
        final ChatChannel channel = new ChatChannel("aChannel");
        underTest.save(channel);
        assertNotNull(underTest.readAggregate("aChannel"));
        underTest.remove(channel);
        assertNull(underTest.readAggregate("aChannel"));
    }

    @Test
    public void shouldNotReadParticipantIfDoesNotExist(){
        assertNull(underTest.read(new ChatParticipant(valueOf(1), "c1", "p1")));
    }
    
    @Test
    public void shouldNotReadChannelIfDoesNotExist(){
        assertNull(underTest.dirtyRead(new ChatChannel("c1")));
    }
}
