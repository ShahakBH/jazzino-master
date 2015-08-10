package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.processor.table.PlayerLastPlayedUpdateRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 5)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class PlayerLastPlayedUpdateProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerLastPlayedUpdateProcessor.class);

    private static final PlayerLastPlayedUpdateRequest TEMPLATE = new PlayerLastPlayedUpdateRequest();

    private final PlayerRepository playerRepository;

    // CGLib constructor
    PlayerLastPlayedUpdateProcessor() {
        this.playerRepository = null;
    }

    @Autowired
    public PlayerLastPlayedUpdateProcessor(final PlayerRepository playerRepository) {
        notNull(playerRepository, "playerRepository may not be null");

        this.playerRepository = playerRepository;
    }

    @EventTemplate
    public PlayerLastPlayedUpdateRequest template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final PlayerLastPlayedUpdateRequest request) {
        LOG.debug("Processing request {}", request);
        if (request == null) {
            return;
        }

        checkInitialisation();

        Player player = playerRepository.findById(request.getPlayerId());
        if (player == null) {
            LOG.warn("Player {} does not exist", request.getPlayerId());
            return;
        }

        player = playerRepository.lock(request.getPlayerId());

        try {
            player.setLastPlayed(request.getLastPlayed());
            playerRepository.saveLastPlayed(player);

        } catch (Exception e) {
            LOG.error("Failed to update player last played for request {}", request);
        }
    }

    private void checkInitialisation() {
        if (playerRepository == null) {
            throw new IllegalStateException("Cannot call logic on CGLib class");
        }
    }
}
