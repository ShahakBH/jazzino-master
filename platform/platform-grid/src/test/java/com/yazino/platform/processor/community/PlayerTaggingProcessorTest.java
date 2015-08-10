package com.yazino.platform.processor.community;

import com.yazino.platform.event.message.PlayerEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerTaggingRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.model.community.PlayerTaggingRequest.Action.ADD;
import static com.yazino.platform.model.community.PlayerTaggingRequest.Action.REMOVE;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerTaggingProcessorTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private QueuePublishingService<PlayerEvent> playerEventService;

    private PlayerTaggingProcessor underTest;

    @Before
    public void setUp() {
        underTest = new PlayerTaggingProcessor(playerRepository, playerEventService);
    }

    @Test
    public void aTagCanBeAdded() {
        final Player player = playerWithTags();
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);

        underTest.processRequest(new PlayerTaggingRequest(PLAYER_ID, "abc", ADD));

        verify(playerRepository).save(playerWithTags("abc"));
    }

    @Test
    public void aTagCanBeRemoved() {
        final Player player = playerWithTags("abc");
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);

        underTest.processRequest(new PlayerTaggingRequest(PLAYER_ID, "abc", REMOVE));

        verify(playerRepository).save(playerWithTags());
    }

    @Test
    public void addingAnExistingTagIsIgnored() {
        final Player player = playerWithTags("abc");
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);

        underTest.processRequest(new PlayerTaggingRequest(PLAYER_ID, "abc", ADD));

        verify(playerRepository, never()).save(any(Player.class));
        verify(playerEventService, never()).send(any(PlayerEvent.class));
    }

    @Test
    public void removingANonExistentTagIsIgnored() {
        final Player player = playerWithTags();
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);

        underTest.processRequest(new PlayerTaggingRequest(PLAYER_ID, "abc", REMOVE));

        verify(playerRepository, never()).save(any(Player.class));
        verify(playerEventService, never()).send(any(PlayerEvent.class));
    }

    @Test
    public void anAddedTagIsSaved() {
        final Player player = playerWithTags();
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);

        underTest.processRequest(new PlayerTaggingRequest(PLAYER_ID, "abc", ADD));

        verify(playerRepository).save(playerWithTags("abc"));
    }

    @Test
    public void anAddedTagSendsAPlayerEvent() {
        final Player player = playerWithTags();
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);

        underTest.processRequest(new PlayerTaggingRequest(PLAYER_ID, "abc", ADD));

        verify(playerEventService).send(anEventWithTags("abc"));
    }

    @Test
    public void aRemovedTagIsSaved() {
        final Player player = playerWithTags("abc");
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);

        underTest.processRequest(new PlayerTaggingRequest(PLAYER_ID, "abc", REMOVE));

        verify(playerRepository).save(playerWithTags());
    }

    @Test
    public void aRemovedTagSendsAPlayerEvent() {
        final Player player = playerWithTags("abc");
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerRepository.lock(PLAYER_ID)).thenReturn(player);

        underTest.processRequest(new PlayerTaggingRequest(PLAYER_ID, "abc", REMOVE));

        verify(playerEventService).send(anEventWithTags());
    }

    private PlayerEvent anEventWithTags(final String... tags) {
        return new PlayerEvent(PLAYER_ID, null, null, newHashSet(tags));
    }

    private Player playerWithTags(final String... tags) {
        final Player player = new Player(PLAYER_ID);
        if (tags != null) {
            player.setTags(new HashSet<>(Arrays.asList(tags)));
        } else {
            player.setTags(new HashSet<String>());
        }
        return player;
    }

}
