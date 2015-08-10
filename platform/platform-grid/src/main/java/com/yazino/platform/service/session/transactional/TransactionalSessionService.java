package com.yazino.platform.service.session.transactional;

import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.processor.session.PlayerSessionWorker;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.LocationChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Delegate class to handle inability to make GS service as @Transactional.
 */
@Service
public class TransactionalSessionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionalSessionService.class);

    private final PlayerSessionRepository playerSessionRepository;

    @SuppressWarnings("UnusedDeclaration")
    TransactionalSessionService() {
        // CGLib constructor

        this.playerSessionRepository = null;
    }

    @Autowired
    public TransactionalSessionService(final PlayerSessionRepository playerSessionRepository) {
        notNull(playerSessionRepository, "playerSessionRepository is null");

        this.playerSessionRepository = playerSessionRepository;
    }

    private void verifyInitialisation() {
        if (playerSessionRepository == null) {
            throw new IllegalStateException(
                    "Class was created with the CGLib constructor and is invalid for direct use");
        }
    }

    @Transactional("spaceTransactionManager")
    public PlayerSessionsSummary updateSession(final LocationChange notification,
                                               final BigDecimal playerBalance) {
        verifyInitialisation();

        notNull(notification, "notification may not be null");

        final Collection<PlayerSession> playerSessions = playerSessionRepository.findAllByPlayer(notification.getPlayerId());
        if (playerSessions.isEmpty()) {
            LOG.debug("No sessions exist for player {}, no update will be performed", notification.getPlayerId());
            return null;
        }

        PlayerSessionsSummary sessionsSummary = null;
        for (PlayerSession playerSession : playerSessions) {
            final PlayerSession lockedSession = playerSessionRepository.lock(notification.getPlayerId(), playerSession.getLocalSessionKey());

            new PlayerSessionWorker(playerSessionRepository).processLocationChange(lockedSession, notification);

            if (playerBalance != null) {
                lockedSession.setBalanceSnapshot(playerBalance);
            }

            LOG.debug("Saving session {}", lockedSession);
            playerSessionRepository.save(lockedSession);

            if (sessionsSummary == null) {
                sessionsSummary = new PlayerSessionsSummary(lockedSession.getNickname(), lockedSession.getPictureUrl(),
                        lockedSession.getBalanceSnapshot(), lockedSession.getLocations());
            } else {
                sessionsSummary.addLocations(lockedSession.getLocations());
            }
        }

        LOG.debug("Saved sessions");
        return sessionsSummary;
    }

    @Transactional("spaceTransactionManager")
    public void updatePlayerInformation(final BigDecimal playerId,
                                        final String playerNickname,
                                        final String pictureUrl) {
        verifyInitialisation();

        notNull(playerId, "playerId may not be null");

        final Collection<PlayerSession> playerSessions = playerSessionRepository.findAllByPlayer(playerId);
        if (playerSessions.isEmpty()) {
            LOG.info("Player {} is not online and the session won't be updated", playerId);
            return;
        }

        for (PlayerSession playerSession : playerSessions) {
            final PlayerSession lockedSession = playerSessionRepository.lock(playerSession.getPlayerId(), playerSession.getLocalSessionKey());
            lockedSession.setNickname(playerNickname);
            lockedSession.setPictureUrl(pictureUrl);

            playerSessionRepository.save(lockedSession);
        }
    }

}
