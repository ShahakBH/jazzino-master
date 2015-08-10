package com.yazino.platform.repository.statistic;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerAchievementsPersistenceRequest;
import com.yazino.platform.persistence.statistic.PlayerStatsDAO;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspacePlayerAchievementsRepository implements PlayerAchievementsRepository {

    private static final int TIMEOUT = 5000;

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;
    private final PlayerStatsDAO playerStatsDao;

    @Autowired
    public GigaspacePlayerAchievementsRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                                 @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                                 final Routing routing,
                                                 final PlayerStatsDAO playerStatsDao) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(routing, "routing may not be null");
        notNull(playerStatsDao, "playerStatsDao may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
        this.playerStatsDao = playerStatsDao;
    }

    @Override
    public PlayerAchievements forPlayer(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        PlayerAchievements result = spaceFor(playerId).readById(PlayerAchievements.class, playerId, playerId, 0, ReadModifiers.DIRTY_READ);
        if (result == null) {
            result = playerStatsDao.getAchievements(playerId);
            if (result != null) {
                spaceFor(playerId).write(result, Lease.FOREVER, TIMEOUT, WriteModifiers.UPDATE_OR_WRITE);
            }
        }

        if (result == null) {
            throw new IllegalArgumentException(String.format("Achievements for player %s not found.", playerId));
        }
        return result;
    }

    @Override
    public void save(final PlayerAchievements playerAchievements) {
        notNull(playerAchievements, "playerAchievements may not be null");

        spaceFor(playerAchievements.getPlayerId()).writeMultiple(
                new Object[]{playerAchievements, new PlayerAchievementsPersistenceRequest(playerAchievements.getPlayerId())},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    private GigaSpace spaceFor(final BigDecimal playerId) {
        if (routing.isRoutedToCurrentPartition(playerId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }
}
