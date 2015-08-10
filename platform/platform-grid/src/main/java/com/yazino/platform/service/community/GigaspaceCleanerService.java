package com.yazino.platform.service.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class GigaspaceCleanerService {
    private final GigaSpace gigaSpace;
    private final PlayerSessionRepository playerSessionRepository;

    GigaspaceCleanerService() {
        this.gigaSpace = null;
        this.playerSessionRepository = null;
    }

    @Autowired(required = true)
    public GigaspaceCleanerService(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                   final PlayerSessionRepository playerSessionRepository) {
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(playerSessionRepository, "playerSessionRepository may not be null");

        this.gigaSpace = gigaSpace;
        this.playerSessionRepository = playerSessionRepository;
    }

    public void removeOfflinePlayers() {
        final Player[] players = gigaSpace.readMultiple(new Player(), Integer.MAX_VALUE);
        for (Player player : players) {
            if (!playerSessionRepository.isOnline(player.getPlayerId())) {
                remove(player.getPlayerId());
            }
        }
    }

    private void remove(final BigDecimal playerId) {
        gigaSpace.clear(new Player(playerId));
        gigaSpace.clear(new PlayerAchievements(playerId));
        gigaSpace.clear(new PlayerLevels(playerId));
    }
}
