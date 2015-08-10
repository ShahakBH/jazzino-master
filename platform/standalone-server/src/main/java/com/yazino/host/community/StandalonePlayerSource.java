package com.yazino.host.community;

import com.yazino.model.StandalonePlayer;
import com.yazino.model.StandalonePlayerLoader;
import com.yazino.model.StandalonePlayerService;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import org.joda.time.DateTime;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.platform.model.community.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.math.BigDecimal.valueOf;

@Component
public class StandalonePlayerSource implements StandalonePlayerService, InitializingBean {

    public static final int INITIAL_BALANCE = 1000;
    private final StandalonePlayerRepository playerRepository;
//    private final PlayerService playerService;
    private int currentId;

    private Map<BigDecimal, StandalonePlayer> players = new ConcurrentHashMap<BigDecimal, StandalonePlayer>();

    @Autowired
    public StandalonePlayerSource(final StandalonePlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public StandalonePlayer findById(final BigDecimal playerId) {
        return players.get(playerId);
    }

    public void save(final StandalonePlayer player) {
        players.put(player.getPlayerId(), player);
        final Player hostPlayer = new Player(player.getPlayerId(),
                player.getName(), player.getPlayerId(), "pic", null, new DateTime(), null);
        playerRepository.save(hostPlayer);
    }

    @Override
    public List<StandalonePlayer> findAll() {
        return new ArrayList<StandalonePlayer>(players.values());
    }

    @Override
    public BigDecimal createPlayer(final String name) {
        currentId++;
        final BigDecimal playerId = valueOf(currentId);
        save(new StandalonePlayer(playerId, name, valueOf(INITIAL_BALANCE)));
        return playerId;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final StandalonePlayerLoader loader = new StandalonePlayerLoader();
        final List<StandalonePlayer> standalonePlayers = loader.loadPlayers();
        for (StandalonePlayer standalonePlayer : standalonePlayers) {
            final int id = standalonePlayer.getPlayerId().intValue();
            if (id > currentId) {
                currentId = id;
            }
            save(standalonePlayer);
        }
        final Map<BigDecimal, Set<BigDecimal>> relationships = loader.getRelationships();
        for (BigDecimal playerId : relationships.keySet()) {
            final Player player = playerRepository.findById(playerId);
            for (BigDecimal friendId : relationships.get(playerId)) {
                final Player friend = playerRepository.findById(friendId);
                final Relationship relationship = new Relationship(friend.getName(), RelationshipType.FRIEND);
                player.setRelationship(friendId, relationship);
            }
            playerRepository.save(player);
        }
    }
}
