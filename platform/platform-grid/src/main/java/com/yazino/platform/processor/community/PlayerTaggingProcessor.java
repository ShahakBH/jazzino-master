package com.yazino.platform.processor.community;

import com.yazino.platform.event.message.PlayerEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerTaggingRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashSet;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class PlayerTaggingProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerTaggingProcessor.class);

    private final PlayerRepository playerRepository;
    private final QueuePublishingService<PlayerEvent> playerEventService;

    @Autowired
    public PlayerTaggingProcessor(final PlayerRepository playerRepository,
                                  @Qualifier("playerEventQueuePublishingService") final QueuePublishingService<PlayerEvent> playerEventService) {
        notNull(playerRepository, "playerRepository may not be null");
        notNull(playerEventService, "playerEventService may not be null");

        this.playerRepository = playerRepository;
        this.playerEventService = playerEventService;
    }

    @EventTemplate
    public PlayerTaggingRequest template() {
        return new PlayerTaggingRequest();
    }

    @SpaceDataEvent
    public void processRequest(final PlayerTaggingRequest request) {
        notNull(request, "request is null");
        LOG.debug("processRequest: {}", request);
        try {
            Player player = playerRepository.findById(request.getPlayerId());

            if (player == null) {
                LOG.warn("Player {} does not exist", request.getPlayerId());
                return;
            }

            player = playerRepository.lock(request.getPlayerId());

            if (player.getTags() == null) {
                LOG.debug("Player {} does not have tags (yet)");
                player.setTags(new HashSet<String>());
            }

            boolean saveRequired = false;
            if (request.getAction() != null && request.getAction() == PlayerTaggingRequest.Action.REMOVE) {
                if (!player.getTags().contains(request.getTag())) {
                    LOG.debug("Player {} is already not tagged with {}. Ignoring...", request.getPlayerId(), request.getTag());
                } else {
                    player.getTags().remove(request.getTag());
                    saveRequired = true;
                }

            } else {
                if (player.getTags().contains(request.getTag())) {
                    LOG.debug("Player {} is already tagged with {}. Ignoring...", request.getPlayerId(), request.getTag());
                } else {
                    player.getTags().add(request.getTag());
                    saveRequired = true;
                }
            }

            if (saveRequired) {
                LOG.debug("Saving player {} with tags: {}", player.getPlayerId(), player.getTags());
                playerRepository.save(player);
                playerEventService.send(asEvent(player));
            }

        } catch (Throwable t) {
            LOG.error("Exception processing tag request {}: ", request, t);
        }
    }

    private PlayerEvent asEvent(final Player player) {
        return new PlayerEvent(player.getPlayerId(),
                player.getCreationTime(),
                player.getAccountId(),
                player.getTags());
    }

}
