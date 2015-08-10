package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.UpdatePlayerRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.session.transactional.TransactionalSessionService;
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
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class UpdatePlayerProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(UpdatePlayerProcessor.class);

    private final TransactionalSessionService transactionalSessionService;
    private final PlayerRepository playerRepository;

    private static final UpdatePlayerRequest TEMPLATE = new UpdatePlayerRequest();

    @SuppressWarnings("UnusedDeclaration")
    public UpdatePlayerProcessor() {
        // CGLib constructor

        this.playerRepository = null;
        this.transactionalSessionService = null;
    }

    private void checkForInitialisation() {
        if (playerRepository == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @Autowired
    public UpdatePlayerProcessor(final PlayerRepository playerRepository,
                                 final TransactionalSessionService transactionalSessionService) {
        notNull(playerRepository, "Player Repository may not be null");
        notNull(transactionalSessionService, "transactionalSessionService may not be null");

        this.transactionalSessionService     = transactionalSessionService;
        this.playerRepository = playerRepository;
    }

    @EventTemplate
    public UpdatePlayerRequest template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void processRequest(final UpdatePlayerRequest request) {
        if (request == null) {
            LOG.warn("Null request received");
            return;
        }

        LOG.info("Processing update player request: {}", request);

        try {
            checkForInitialisation();
        } catch (Exception e) {
            LOG.error("Processor failed initialisation checks", e);
        }

        if (playerRepository.findById(request.getPlayerId()) == null) {
            LOG.error("Player not found: " + request.getPlayerId());
            return;
        }

        final Player player = playerRepository.lock(request.getPlayerId());
        player.setName(request.getDisplayName());
        player.setPictureUrl(request.getPictureLocation());
        player.setPaymentPreferences(request.getPaymentPreferences());

        playerRepository.save(player);

        try {
            transactionalSessionService.updatePlayerInformation(
                    request.getPlayerId(), request.getDisplayName(), request.getPictureLocation());

        } catch (Exception e) {
            LOG.error("Session update failed for request " + request, e);
        }
    }

}
