package com.yazino.platform.repository.statistic;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.model.statistic.PlayerLevelsPersistenceRequest;
import com.yazino.platform.persistence.statistic.PlayerStatsDAO;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspacePlayerLevelsRepository implements PlayerLevelsRepository {

    private static final int TIMEOUT = 5000;

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;
    private final PlayerStatsDAO playerStatsDao;

    @Autowired
    public GigaspacePlayerLevelsRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
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
    public PlayerLevels forPlayer(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        PlayerLevels result = spaceFor(playerId).readById(PlayerLevels.class, playerId, playerId, 0, ReadModifiers.DIRTY_READ);
        if (result == null) {
            result = playerStatsDao.getLevels(playerId);
            if (result != null) {
                spaceFor(playerId).write(result, Lease.FOREVER, TIMEOUT, WriteModifiers.UPDATE_OR_WRITE);
            }
        }

        if (result == null) {
            throw new IllegalArgumentException(String.format("Levels for player %s not found.", playerId));
        }
        return result;
    }

    @Override
    public void save(final PlayerLevels playerLevels) {
        notNull(playerLevels, "playerLevels may not be null");

        spaceFor(playerLevels.getPlayerId()).writeMultiple(
                new Object[]{playerLevels, new PlayerLevelsPersistenceRequest(playerLevels.getPlayerId())},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    private GigaSpace spaceFor(final BigDecimal playerId) {
        if (routing.isRoutedToCurrentPartition(playerId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }
}
