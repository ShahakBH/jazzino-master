package com.yazino.platform.repository.community;

import com.gigaspaces.client.ReadModifiers;
import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.model.community.PlayerTrophyPersistenceRequest;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A Gigaspace implementation of {@link PlayerTrophyRepository}
 */
@Repository
public class GigaspacePlayerTrophyRepository implements PlayerTrophyRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspacePlayerTrophyRepository.class);

    private static final String SELECT_BY_TROPHY_QUERY = "trophyId=%s ORDER BY awardTime DESC";

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;

    @Autowired
    public GigaspacePlayerTrophyRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                           @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                           final Routing routing) {
        notNull(localGigaSpace, "localGigaSpace must not be null");
        notNull(globalGigaSpace, "globalGigaSpace must not be null");
        notNull(routing, "routing must not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
    }

    @Override
    public void save(final PlayerTrophy playerTrophy) {
        notNull(playerTrophy, "playerTrophy cannot be null");

        spaceFor(playerTrophy.getPlayerId()).writeMultiple(
                new Object[]{playerTrophy, new PlayerTrophyPersistenceRequest(playerTrophy.getPlayerId(), playerTrophy)});
        LOG.debug("Wrote player trophy {}", playerTrophy);
    }

    @Override
    public List<PlayerTrophy> findWinnersByTrophyId(final BigDecimal trophyId,
                                                    final int maxResults) {
        notNull(trophyId, "trophyId may not be null");

        final String formatted = String.format(SELECT_BY_TROPHY_QUERY, trophyId);
        final SQLQuery<PlayerTrophy> sqlQuery = new SQLQuery<PlayerTrophy>(PlayerTrophy.class, formatted);
        final PlayerTrophy[] playerTrophys = globalGigaSpace.readMultiple(sqlQuery, maxResults, ReadModifiers.DIRTY_READ);

        final List<PlayerTrophy> trophies = Arrays.asList(playerTrophys);
        LOG.debug("Query {} produced {}", formatted, trophies);
        return trophies;
    }

    @Override
    public Collection<PlayerTrophy> findPlayersTrophies(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final PlayerTrophy template = new PlayerTrophy();
        template.setPlayerId(playerId);
        return Arrays.asList(spaceFor(playerId).readMultiple(template, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ));
    }

    private GigaSpace spaceFor(final BigDecimal playerId) {
        if (routing.isRoutedToCurrentPartition(playerId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }

}
