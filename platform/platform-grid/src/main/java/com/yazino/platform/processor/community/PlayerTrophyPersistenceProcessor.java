package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.model.community.PlayerTrophyPersistenceRequest;
import com.yazino.platform.persistence.community.PlayerTrophyDAO;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class will process {@link PlayerTrophyPersistenceRequest} objects that appear in GigaSpace.
 * It will use the underlying {@link PlayerTrophyDAO} to do the persistence.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class PlayerTrophyPersistenceProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerTrophyPersistenceProcessor.class);

    private static final PlayerTrophyPersistenceRequest TEMPLATE = new PlayerTrophyPersistenceRequest();

    private PlayerTrophyDAO playerTrophyDAO = PlayerTrophyDAO.NULL;

    @SpaceDataEvent
    public void processPlayerTrophyPersistenceRequest(final PlayerTrophyPersistenceRequest request) {
        notNull(request, "request must not be null");
        final PlayerTrophy playerTrophy = request.getPlayerTrophy();

        if (LOG.isDebugEnabled()) {
            LOG.debug("processing request " + request.getPlayerId() + " with player trophy " + playerTrophy);
        }
        try {
            playerTrophyDAO.insert(playerTrophy);
            if (LOG.isDebugEnabled()) {
                LOG.debug("inserted player trophy using dao " + playerTrophyDAO);
            }

        } catch (Throwable e) {
            LOG.error("Failed to write player trophy " + playerTrophy + "to dao because " + e, e);
        }
    }

    @Autowired(required = true)
    public void setPlayerTrophyDAO(final PlayerTrophyDAO playerTrophyDAO) {
        notNull(playerTrophyDAO, "playerTrophyDAO must not be null");
        this.playerTrophyDAO = playerTrophyDAO;
    }

    @EventTemplate
    public PlayerTrophyPersistenceRequest template() {
        return TEMPLATE;
    }


}
