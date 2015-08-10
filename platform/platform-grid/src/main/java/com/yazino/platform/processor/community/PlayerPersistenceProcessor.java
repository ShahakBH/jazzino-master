package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerPersistenceRequest;
import com.yazino.platform.persistence.community.PlayerDAO;
import com.yazino.platform.repository.community.PlayerRepository;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class PlayerPersistenceProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerPersistenceProcessor.class);
    private PlayerRepository playerRepository;
    private GigaSpace communityGigaSpace;
    private PlayerDAO playerDAO;

    @Autowired(required = true)
    public void setPlayerRepository(final PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Autowired(required = true)
    @Qualifier("gigaSpace")
    public void setCommunityGigaSpace(final GigaSpace communityGigaSpace) {
        this.communityGigaSpace = communityGigaSpace;
    }

    @EventTemplate
    public PlayerPersistenceRequest template() {
        return new PlayerPersistenceRequest();
    }

    @Autowired(required = true)
    public void setPlayerDAO(final PlayerDAO playerDAO) {
        this.playerDAO = playerDAO;
    }

    @SpaceDataEvent
    @Transactional
    public PlayerPersistenceRequest processRequest(final PlayerPersistenceRequest request) {
        notNull(request, "request is null");
        notNull(playerDAO, "Player DAO is null!");
        if (LOG.isDebugEnabled()) {
            LOG.debug("processRequest: " + ReflectionToStringBuilder.reflectionToString(request.getPlayerId()));
        }
        try {
            final Player player = playerRepository.findById(request.getPlayerId());
            if (player == null) {
                LOG.error("Trying to persist player not in space or db! " + request.getPlayerId());
                tryRemovingMatchingRequests(request);
                return null;
            }
            playerDAO.save(player);
        } catch (Throwable t) {
            LOG.error("Exception saving player see stack trace: ", t);
            return requestInErrorState(request);
        }
        tryRemovingMatchingRequests(request);
        return null;
    }

    private PlayerPersistenceRequest requestInErrorState(final PlayerPersistenceRequest request) {
        request.setStatus(PlayerPersistenceRequest.STATUS_ERROR);
        return request;
    }

    private void tryRemovingMatchingRequests(final PlayerPersistenceRequest request) {
        try {
            final PlayerPersistenceRequest otherRequestsTemplate = new PlayerPersistenceRequest(request.getPlayerId());
            communityGigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);
            otherRequestsTemplate.setStatus(PlayerPersistenceRequest.STATUS_ERROR);
            communityGigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);
        } catch (Throwable t) {
            LOG.error("Exception removing matching requests see stack trace: ", t);
        }
    }
}
