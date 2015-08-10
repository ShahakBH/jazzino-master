package com.yazino.platform.processor.community;

import com.yazino.platform.community.RelatedPlayer;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.model.community.FriendRegistrationRequest;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.RelationshipActionRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 5)
public class FriendRegistrationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FriendRegistrationProcessor.class);

    private final PlayerRepository playerRepository;

    @Autowired
    public FriendRegistrationProcessor(final PlayerRepository playerRepository) {
        notNull(playerRepository, "playerRepository may not be null");

        this.playerRepository = playerRepository;
    }

    @EventTemplate
    public FriendRegistrationRequest getEventTemplate() {
        return new FriendRegistrationRequest();
    }

    @SpaceDataEvent
    public void process(final FriendRegistrationRequest request) {
        LOG.debug("Processing request: {}", request);

        if (request == null || request.getFriends() == null || request.getFriends().isEmpty()) {
            return;
        }

        final Player player = playerRepository.findById(request.getPlayerId());
        if (player == null) {
            LOG.warn("Friend registration requested for invalid player ID {}", request.getPlayerId());
            return;
        }

        final Set<RelationshipActionRequest> relationshipActionRequests = generatedActionsFor(player, request.getFriends());
        if (!relationshipActionRequests.isEmpty()) {
            playerRepository.requestRelationshipChanges(relationshipActionRequests);
        }
    }

    private Set<RelationshipActionRequest> generatedActionsFor(final Player player,
                                                               final Set<BigDecimal> friends) {
        final Set<RelationshipActionRequest> relationshipActionRequests = new HashSet<>();
        final Set<RelatedPlayer> batch = new HashSet<>();
        for (final BigDecimal friendPlayerId : friends) {
            final Player friend = playerRepository.findById(friendPlayerId);
            if (friend == null) {
                continue;
            }

            if (friend.getRelationshipTo(player.getPlayerId()) == null) {
                relationshipActionRequests.add(new RelationshipActionRequest(friend.getPlayerId(),
                        player.getPlayerId(), player.getName(), RelationshipAction.SET_EXTERNAL_FRIEND, true));
            }
            if (player.getRelationshipTo(friend.getPlayerId()) == null) {
                batch.add(new RelatedPlayer(friend.getPlayerId(), friend.getName(),
                        RelationshipAction.SET_EXTERNAL_FRIEND, false));
            }
        }

        if (!batch.isEmpty()) {
            relationshipActionRequests.add(new RelationshipActionRequest(player.getPlayerId(), batch));
        }
        return relationshipActionRequests;
    }
}
