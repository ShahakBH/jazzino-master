package com.yazino.platform.processor.community;

import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.model.community.FriendRegistrationRequest;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.RelationshipActionRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.CREDITCARD;
import static com.yazino.platform.community.RelationshipType.FRIEND;
import static com.yazino.platform.reference.Currency.GBP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FriendRegistrationProcessorTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);

    @Mock
    private PlayerRepository playerRepository;

    private FriendRegistrationProcessor underTest;

    @Before
    public void setUp() {
        underTest = new FriendRegistrationProcessor(playerRepository);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullPlayerRepository() {
        new FriendRegistrationProcessor(null);
    }

    @Test
    public void theProcessorMatchesAnEmptyRequest() {
        assertThat(underTest.getEventTemplate(), is(equalTo(new FriendRegistrationRequest())));
    }

    @Test
    public void friendsRegistrationBindsExistingPlayersInBothDirections() {
        final BigDecimal newFriendId = BigDecimal.valueOf(1001);

        when(playerRepository.findById(newFriendId)).thenReturn(aPlayer(newFriendId));
        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer(PLAYER_ID));

        underTest.process(new FriendRegistrationRequest(PLAYER_ID, newHashSet(newFriendId)));

        verify(playerRepository).requestRelationshipChanges(newHashSet(
                new RelationshipActionRequest(newFriendId, PLAYER_ID, "player" + PLAYER_ID, RelationshipAction.SET_EXTERNAL_FRIEND, true),
                new RelationshipActionRequest(PLAYER_ID, newFriendId, "player" + newFriendId, RelationshipAction.SET_EXTERNAL_FRIEND, false)));
    }

    @Test
    public void friendRegistrationIgnoresANullRequest() {
        underTest.process(null);

        verifyZeroInteractions(playerRepository);
    }

    @Test
    public void friendRegistrationIgnoresAnEmptyFriendsList() {
        underTest.process(new FriendRegistrationRequest(PLAYER_ID, Collections.<BigDecimal>emptySet()));

        verifyZeroInteractions(playerRepository);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void friendRegistrationIgnoresExistingRelationships() {
        final BigDecimal newFriendId = BigDecimal.valueOf(1001);

        final Player friend = aPlayer(newFriendId);
        friend.setRelationship(PLAYER_ID, new Relationship("zeko", FRIEND));
        final Player player = aPlayer(PLAYER_ID);
        player.setRelationship(newFriendId, new Relationship("zeko", FRIEND));
        when(playerRepository.findById(newFriendId)).thenReturn(friend);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);

        underTest.process(new FriendRegistrationRequest(PLAYER_ID, newHashSet(newFriendId)));

        verify(playerRepository).findById(newFriendId);
        verify(playerRepository, never()).requestRelationshipChanges(anySet());
    }


    @SuppressWarnings("unchecked")
    @Test
    public void friendRegistrationPerformsNoActionsIfThePlayerIdIsInvalid() {
        when(playerRepository.findById(PLAYER_ID)).thenReturn(null);
        final BigDecimal friendId = BigDecimal.valueOf(1001);
        when(playerRepository.findById(friendId)).thenReturn(new Player(friendId));

        underTest.process(new FriendRegistrationRequest(PLAYER_ID, newHashSet(friendId)));

        verify(playerRepository).findById(PLAYER_ID);
        verify(playerRepository, never()).requestRelationshipChanges(anySet());
    }

    private Player aPlayer(final BigDecimal newPlayerId) {
        final Player player = new Player();
        player.setPlayerId(newPlayerId);
        player.setAccountId(BigDecimal.ONE);
        player.setPictureUrl("aPictureUrl");
        player.setName("player" + newPlayerId);
        player.setPaymentPreferences(new PaymentPreferences(GBP, CREDITCARD, "UK"));
        player.setCreationTime(new DateTime());
        return player;
    }
}
