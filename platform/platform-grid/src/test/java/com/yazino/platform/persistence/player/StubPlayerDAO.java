package com.yazino.platform.persistence.player;

import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.persistence.community.PlayerDAO;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.*;

public class StubPlayerDAO implements PlayerDAO {

    private final Set<Player> players = new HashSet<>();

    public void clear() {
        players.clear();
    }

    @Override
    public void save(final Player p) {
        players.add(p);
    }

    @Override
    public Player findById(final BigDecimal playerId) {
        for (Player player : players) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public Collection<Player> findByIds(final Set<BigDecimal> playerIds) {
        final List<Player> matchingPlayers = new ArrayList<>();
        for (Player player : players) {
            if (playerIds.contains(player.getPlayerId())) {
                matchingPlayers.add(player);
            }
        }
        return matchingPlayers;
    }

    @Override
    public Set<Player> findAll() {
        return Collections.unmodifiableSet(players);
    }


    @Override
    public PagedData<Player> findByName(final String name, final int page, final BigDecimal playerIdToExclude) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void updateLastPlayedTs(final BigDecimal playerId, final DateTime lastPlayedTs) {
        throw new UnsupportedOperationException("Unimplemented");
    }

}
