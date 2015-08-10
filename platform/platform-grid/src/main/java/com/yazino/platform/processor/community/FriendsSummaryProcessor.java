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
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.symmetricDifference;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 3)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class FriendsSummaryProcessor {

    public static final int MAX_ITEMS = 20;
    private final PlayerRepository playerRepository;
    private final PlayerSessionRepository playerSessionRepository;
    private final DocumentDispatcher dispatcher;
    private final FriendsSummaryDocumentBuilder documentBuilder;

    @Autowired
    public FriendsSummaryProcessor(final PlayerRepository playerRepository,
                                   final PlayerSessionRepository playerSessionRepository,
                                   final FriendsSummaryDocumentBuilder documentBuilder,
                                   @Qualifier("documentDispatcher") final DocumentDispatcher dispatcher) {
        this.playerRepository = playerRepository;
        this.playerSessionRepository = playerSessionRepository;
        this.dispatcher = dispatcher;
        this.documentBuilder = documentBuilder;
    }

    @EventTemplate
    public FriendsSummaryRequest getEventTemplate() {
        return new FriendsSummaryRequest();
    }

    @SpaceDataEvent
    public void process(final FriendsSummaryRequest request) {
        final Player player = player(request.getPlayerId());
        final Set<BigDecimal> friends = newHashSet(player.retrieveFriends().keySet());
        final Set<BigDecimal> online = playerSessionRepository.findOnlinePlayers(friends);
        final Set<BigDecimal> offlineSubList = subList(symmetricDifference(friends, online));
        final Set<BigDecimal> onlineSubList = subList(online);
        final Set<BigDecimal> requests = findFriendRequests(player);

        final Document document = documentBuilder.build(online.size(),
                friends.size(),
                onlineSubList,
                offlineSubList,
                requests);

        dispatcher.dispatch(document, request.getPlayerId());
    }

    private Set<BigDecimal> findFriendRequests(final Player player) {
        final Set<BigDecimal> friendRequests = new HashSet<BigDecimal>();
        final Map<BigDecimal, Relationship> relationships = player.retrieveRelationships();
        for (BigDecimal friendId : relationships.keySet()) {
            final Relationship rel = relationships.get(friendId);
            if (rel.getType() == RelationshipType.INVITATION_RECEIVED) {
                friendRequests.add(friendId);
            }
        }
        return friendRequests;
    }

    private Set<BigDecimal> subList(final Collection<BigDecimal> original) {
        final List<BigDecimal> items = new ArrayList<BigDecimal>(original);
        final Set<BigDecimal> result = new HashSet<BigDecimal>();
        for (int i = 0; i < MAX_ITEMS; i++) {
            if (items.size() > 0) {
                result.add(items.remove(0));
            }
        }
        return result;
    }

    private Player player(final BigDecimal playerId) {
        final Player player = playerRepository.findById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Couldn't load player " + playerId);
        }
        return player;
    }
}
