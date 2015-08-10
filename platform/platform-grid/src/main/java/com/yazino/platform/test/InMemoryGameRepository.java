package com.yazino.platform.test;

import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.game.api.GameMetaData;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;

import java.util.*;

public class InMemoryGameRepository implements GameRepository {
    private Map<String, GameRules> knownRules = new HashMap<String, GameRules>();
    private Map<String, Boolean> availability = new HashMap<String, Boolean>();

    public InMemoryGameRepository(final GameRules rules) {
        addGameRules(rules);
    }

    private void addGameRules(final GameRules rules) {
        knownRules.put(rules.getGameType(), rules);
        availability.put(rules.getGameType(), true);
    }

    public InMemoryGameRepository(final List<GameRules> rules) {
        for (GameRules rule : rules) {
            addGameRules(rule);
        }
    }

    public GameRules getGameRules(final String gameType) {
        if (knownRules.containsKey(gameType)) {
            return knownRules.get(gameType);
        }
        throw new IllegalArgumentException("Unknown game " + gameType);
    }

    @Override
    public GameType getGameTypeFor(final String gameTypeId) {
        final GameRules gameRules = getGameRules(gameTypeId);
        if (gameRules != null) {
            return dummyGameTypeFor(gameRules);
        }
        return null;
    }

    private GameType dummyGameTypeFor(final GameRules gameRules) {
        return new GameType(gameRules.getGameType(), gameRules.getGameType(), Collections.<String>emptySet());
    }

    @Override
    public GameMetaData getMetaDataFor(final String gameTypeId) {
        final GameRules gameRules = getGameRules(gameTypeId);
        if (gameRules != null) {
            return gameRules.getMetaData();
        }
        return null;
    }

    public boolean isGameAvailable(final String gameType) {
        final Boolean available = availability.get(gameType);
        if (available != null) {
            return available;
        }
        return false;
    }

    public void setGameAvailable(final String gameType,
                                 final boolean isAvailable) {
        availability.put(gameType, null);
    }

    public Set<GameTypeInformation> getAvailableGameTypes() {
        final Set<GameTypeInformation> info = new HashSet<GameTypeInformation>();
        for (String gameTypeId : knownRules.keySet()) {
            final Boolean available = availability.get(gameTypeId);
            if (available != null) {
                info.add(new GameTypeInformation(dummyGameTypeFor(knownRules.get(gameTypeId)), available));
            } else {
                info.add(new GameTypeInformation(dummyGameTypeFor(knownRules.get(gameTypeId)), false));
            }
        }
        return info;
    }
}
