package com.yazino.platform.processor.community;


import com.yazino.platform.community.RelatedPlayer;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PublishStatusRequest;
import com.yazino.platform.model.community.PublishStatusRequestType;
import com.yazino.platform.model.community.RelationshipActionRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.community.PlayerWorker;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * The processor simulates work done no un-processed Data object. The processData
 * accepts a Data object, simulate work by sleeping, and then sets the processed
 * flag to true and returns the processed Data.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 3)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class RelationshipRequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RelationshipRequestProcessor.class);

    @EventTemplate
    public RelationshipActionRequest template() {
        return new RelationshipActionRequest();
    }

    private PlayerRepository playerRepository;

    @Autowired
    public void setPlayerRepository(final PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @SpaceDataEvent
    public Player processRelationshipRequest(final RelationshipActionRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("entering processRelationshipRequest " + ToStringBuilder.reflectionToString(request));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("processing relationship request for " + request.getPlayerId());
        }
        Player player = playerRepository.findById(request.getPlayerId());
        if (player == null) {
            LOG.error("player not found " + request.getPlayerId());
            return player;
        }// no such player
        if (request.getRelatedPlayers() == null) {
            LOG.error("no related players defined in request");
        }
        final PlayerWorker pw = new PlayerWorker();
        final HashMap<BigDecimal, Relationship> newRelationships = new HashMap<BigDecimal, Relationship>();
        for (RelatedPlayer rp : request.getRelatedPlayers()) {
            if (request.getPlayerId().equals(rp.getPlayerId())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("player wants to modify a self-relationship: " + request.getPlayerId());
                }
                continue;
            }// self-relationship
            final RelationshipType currentRelationshipType = pw.getCurrentRelationshipType(player, rp.getPlayerId());
            final RelationshipType newRelationshipType = pw.calculateNewRelationshipType(currentRelationshipType,
                    rp.getRequestedAction(), rp.isProcessingInverseSide());
            if (newRelationshipType != currentRelationshipType) {
                final Relationship resultingRelationship = new Relationship(rp.getPlayerName(), newRelationshipType);
                newRelationships.put(rp.getPlayerId(), resultingRelationship);
            }
        }
        if (newRelationships.size() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Saving player and asking to publish relationships ");
            }
            player = playerRepository.lock(request.getPlayerId());
            for (BigDecimal id : newRelationships.keySet()) {
                player.setRelationship(id, newRelationships.get(id));
            }
            playerRepository.save(player);
            playerRepository.savePublishStatusRequest(new PublishStatusRequest(
                    player.getPlayerId(), PublishStatusRequestType.RELATIONSHIPS));
            return player;
        }
        return null;
    }
}
