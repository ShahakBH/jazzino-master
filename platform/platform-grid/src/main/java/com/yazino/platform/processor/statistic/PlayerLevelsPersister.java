package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.model.statistic.PlayerLevelsPersistenceRequest;
import com.yazino.platform.persistence.statistic.PlayerStatsDAO;
import com.yazino.platform.processor.PersistenceRequest;
import com.yazino.platform.processor.Persister;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service("playerLevelsPersister")
@Qualifier("persister")
public class PlayerLevelsPersister implements Persister<BigDecimal> {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerLevelsPersister.class);

    private final PlayerStatsDAO playerStatsDao;
    private final PlayerLevelsRepository playerLevelsRepository;

    @Autowired
    public PlayerLevelsPersister(final PlayerStatsDAO playerStatsDao,
                                 final PlayerLevelsRepository playerLevelsRepository) {
        notNull(playerStatsDao, "playerStatsDao is null");
        notNull(playerLevelsRepository, "playerLevelsRepository is null");

        this.playerStatsDao = playerStatsDao;
        this.playerLevelsRepository = playerLevelsRepository;
    }

    @Override
    public Class<? extends PersistenceRequest<BigDecimal>> getPersistenceRequestClass() {
        return PlayerLevelsPersistenceRequest.class;
    }

    public PersistenceRequest<BigDecimal> persist(final PersistenceRequest<BigDecimal> request) {
        if (request == null || request.getObjectId() == null) {
            LOG.warn("Invalid request received: {}", request);
            return null;
        }
        LOG.debug("Received persistence request {}", request);

        final PlayerLevels playerLevels;
        try {
            playerLevels = playerLevelsRepository.forPlayer(request.getObjectId());

        } catch (IllegalArgumentException e) {
            LOG.error("Couldn't find player levels for player {}", request.getObjectId());
            return null;
        }

        try {
            playerStatsDao.saveLevels(playerLevels);
            return null;

        } catch (Exception e) {
            request.setStatus(PersistenceRequest.Status.ERROR);
            LOG.error("Levels persistence failed for player {}", request.getObjectId(), e);
            return request;
        }
    }
}
