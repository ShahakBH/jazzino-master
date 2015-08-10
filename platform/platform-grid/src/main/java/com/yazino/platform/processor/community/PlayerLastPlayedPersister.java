package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.persistence.community.PlayerDAO;
import com.yazino.platform.processor.PersistenceRequest;
import com.yazino.platform.processor.Persister;
import com.yazino.platform.repository.community.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service("playerLastPlayedPersister")
@Qualifier("persister")
public class PlayerLastPlayedPersister implements Persister<BigDecimal> {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerLastPlayedPersister.class);

    private final PlayerRepository playerRepository;
    private final PlayerDAO playerDao;

    @Autowired
    public PlayerLastPlayedPersister(final PlayerRepository playerRepository,
                                     final PlayerDAO playerDao) {
        notNull(playerRepository, "playerRepository can not be null");
        notNull(playerDao, "PlayerDao my not be null");

        this.playerRepository = playerRepository;
        this.playerDao = playerDao;
    }

    @Override
    public PersistenceRequest<BigDecimal> persist(final PersistenceRequest<BigDecimal> persistenceRequest) {
        LOG.debug("Attempting update player last played for {}", persistenceRequest);

        PersistenceRequest<BigDecimal> afterProcessing = null;

        try {
            final Player player = playerRepository.findById(persistenceRequest.getObjectId());
            if (player == null) {
                LOG.warn("Player {} not found", persistenceRequest.getObjectId());
                return null;
            }

            playerDao.updateLastPlayedTs(player.getPlayerId(), player.getLastPlayed());

        } catch (Exception e) {
            LOG.error("Failed to persist player last played for {}", persistenceRequest);

            afterProcessing = persistenceRequest;
            afterProcessing.setStatus(PersistenceRequest.Status.ERROR);
        }

        return afterProcessing;
    }

    @Override
    public Class<? extends PersistenceRequest<BigDecimal>> getPersistenceRequestClass() {
        return PlayerLastPlayedPersistenceRequest.class;
    }
}
