package com.yazino.game.api;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Collection;
import java.util.Map;

public class GameCreationContext {
    private final Collection<PlayerAtTableInformation> playersAtTable;
    private final GamePlayerWalletFactory gamePlayerWalletFactory;
    private final String auditLabel;
    private final long gameId;
    private final Map<String, String> properties;
    private final long increment;

    public GameCreationContext(final long gameId,
                               final GamePlayerWalletFactory gamePlayerWalletFactory,
                               final Map<String, String> properties,
                               final String auditLabel,
                               final long increment,
                               final Collection<PlayerAtTableInformation> playersAtTable) {
        notNull(gamePlayerWalletFactory, "gamePlayerWalletFactory is null");
        notNull(properties, "properties is null");
        notNull(auditLabel, "auditLabel is null");
        notNull(playersAtTable, "playersAtTable is null");
        this.increment = increment;
        this.playersAtTable = playersAtTable;
        this.gamePlayerWalletFactory = gamePlayerWalletFactory;
        this.auditLabel = auditLabel;
        this.gameId = gameId;
        this.properties = properties;
    }

    public Collection<PlayerAtTableInformation> getPlayersAtTableInformation() {
        return playersAtTable;
    }

    public GamePlayerWalletFactory getGameWalletFactory() {
        return gamePlayerWalletFactory;
    }

    public String getAuditLabel() {
        return auditLabel;
    }

    public long getGameId() {
        return gameId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public long getIncrement() {
        return increment;
    }

    private static void notNull(final Object object,
                                final String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this);
    }
}
