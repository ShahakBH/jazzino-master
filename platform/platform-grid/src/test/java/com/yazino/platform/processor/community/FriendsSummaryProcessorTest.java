package com.yazino.platform.processor.community;

import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.model.community.FriendsSummaryDocumentBuilder;
import com.yazino.platform.model.community.FriendsSummaryRequest;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.*;

import static java.math.BigDecimal.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FriendsSummaryProcessorTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private FriendsSummaryProcessor underTest;
    private PlayerRepository playerRepository;
    private DocumentDispatcher dispatcher;
    private PlayerSessionRepository playerSessionRepository;
    private FriendsSummaryDocumentBuilder documentBuilder;

    @Before
    public void setUp() throws Exception {
        playerRepository = mock(PlayerRepository.class);
        dispatcher = mock(DocumentDispatcher.class);
        playerSessionRepository = mock(PlayerSessionRepository.class);
        documentBuilder = mock(FriendsSummaryDocumentBuilder.class);
        underTest = new FriendsSummaryProcessor(playerRepository, playerSessionRepository, documentBuilder, dispatcher);
    }

    @Test
    public void publishesSummaryToPlayer() {
        BigDecimal friend1 = valueOf(10);
        BigDecimal friend2 = valueOf(11);
        BigDecimal friend3 = valueOf(12);
        BigDecimal request1 = valueOf(13);
        BigDecimal request2 = valueOf(14);

        HashSet<BigDecimal> friends = set(friend1, friend2, friend3);
        HashSet<BigDecimal> online = set(friend2);
        HashSet<BigDecimal> requests = set(request1, request2);

        HashMap<BigDecimal, Relationship> relationships = new HashMap<BigDecimal, Relationship>();
        relationships.put(friend1, new Relationship("friend1", RelationshipType.FRIEND));
        relationships.put(friend2, new Relationship("friend2", RelationshipType.FRIEND));
        relationships.put(friend3, new Relationship("friend3", RelationshipType.FRIEND));
        relationships.put(request1, new Relationship("request1", RelationshipType.INVITATION_RECEIVED));
        relationships.put(request2, new Relationship("request2", RelationshipType.INVITATION_RECEIVED));

        Player player = mock(Player.class);
        when(player.retrieveFriends()).thenReturn(friendsAsMap(10, 11, 12));
        when(player.retrieveRelationships()).thenReturn(relationships);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);

        Document document = new Document("aDoc", "abody", Collections.<String, String>emptyMap());
        when(playerSessionRepository.findOnlinePlayers(friends)).thenReturn(online);
        when(documentBuilder.build(eq(online.size()), eq(friends.size()),
                any(Set.class), any(Set.class),
                eq(requests))).thenReturn(document);

        underTest.process(new FriendsSummaryRequest(PLAYER_ID));

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(dispatcher).dispatch(documentCaptor.capture(), eq(PLAYER_ID));
        assertEquals(document.getType(), documentCaptor.getValue().getType());
    }

    private Map<BigDecimal, String> friendsAsMap(final Number... playerIds) {
        Map<BigDecimal, String> friendsMap = new HashMap<BigDecimal, String>();
        for (int i = 0; i < playerIds.length; i++) {
            friendsMap.put(valueOf(playerIds[i].intValue()), "friend" + playerIds[i]);

        }
        return friendsMap;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFilterOnlineAndOfflineFriends() {
        final Set<BigDecimal> friends = ids(1, 100);
        final Set<BigDecimal> online = ids(1, 30);
        final Set<BigDecimal> offline = ids(31, 100);

        Player player = mock(Player.class);
        when(player.retrieveFriends()).thenReturn(friendsAsMap(friends.toArray(new BigDecimal[friends.size()])));
        when(player.retrieveRelationships()).thenReturn(Collections.<BigDecimal, Relationship>emptyMap());
        when(playerRepository.findById(PLAYER_ID)).thenReturn(player);
        when(playerSessionRepository.findOnlinePlayers(friends)).thenReturn(online);

        underTest.process(new FriendsSummaryRequest(PLAYER_ID));

        final ArgumentCaptor<Set> onlineSubListCaptor = ArgumentCaptor.forClass(Set.class);
        final ArgumentCaptor<Set> offlineSubListCaptor = ArgumentCaptor.forClass(Set.class);

        verify(documentBuilder).build(eq(online.size()),
                eq(friends.size()),
                onlineSubListCaptor.capture(),
                offlineSubListCaptor.capture(),
                eq(Collections.<BigDecimal>emptySet()));

        final Set actualOffline = offlineSubListCaptor.getValue();
        assertEquals(20, actualOffline.size());
        assertTrue(offline.containsAll(actualOffline));

        final Set actualOnline = onlineSubListCaptor.getValue();
        assertEquals(20, actualOnline.size());
        assertTrue(online.containsAll(actualOnline));
    }

    private Set<BigDecimal> ids(int from, int to) {
        final Set<BigDecimal> friends = new HashSet<BigDecimal>();
        for (int i = from; i <= to; i++) {
            friends.add(valueOf(i));
        }
        return friends;
    }

    private HashSet<BigDecimal> set(BigDecimal... friends) {
        return new HashSet<BigDecimal>(Arrays.asList(friends));
    }

}
