package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerAchievementsPersistenceRequest;
import com.yazino.platform.persistence.statistic.PlayerStatsDAO;
import com.yazino.platform.processor.PersistenceRequest;
import com.yazino.platform.processor.Persister;
import com.yazino.platform.repository.statistic.PlayerAchievementsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service("playerAchivementsPersister")
@Qualifier("persister")
public class PlayerAchievementsPersister implements Persister<BigDecimal> {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerAchievementsPersister.class);

    private final PlayerStatsDAO playerStatsDao;
    private final PlayerAchievementsRepository playerAchievementsRepository;

    @Autowired
    public PlayerAchievementsPersister(final PlayerStatsDAO playerStatsDao,
                                       final PlayerAchievementsRepository playerAchievementsRepository) {
        notNull(playerStatsDao, "playerStatsDao is null");
        notNull(playerAchievementsRepository, "playerAchievementsRepository is null");

        this.playerStatsDao = playerStatsDao;
        this.playerAchievementsRepository = playerAchievementsRepository;
    }

    @Override
    public Class<? extends PersistenceRequest<BigDecimal>> getPersistenceRequestClass() {
        return PlayerAchievementsPersistenceRequest.class;
    }

    @Override
    public PersistenceRequest<BigDecimal> persist(final PersistenceRequest<BigDecimal> request) {
        if (request == null || request.getObjectId() == null) {
            LOG.warn("Invalid request received: {}", request);
            return null;
        }

        LOG.debug("Received persistence request {}", request);

        final PlayerAchievements playerAchievements;
        try {
            playerAchievements = playerAchievementsRepository.forPlayer(request.getObjectId());

        } catch (IllegalArgumentException e) {
            LOG.error("Couldn't find player achievements for player {}", request.getObjectId());
            return null;
        }

        try {
            playerStatsDao.saveAchievements(playerAchievements);
            return null;

        } catch (Exception e) {
            request.setStatus(PersistenceRequest.Status.ERROR);
            LOG.error("Achievement persistence failed for player {}", request.getObjectId(), e);
            return request;
        }
    }
}
