package com.yazino.platform.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class GameClient implements Serializable {
    private static final long serialVersionUID = 5367538776776464525L;

    private final Map<String, String> clientProperties = new HashMap<String, String>();

    private final String clientId;
    private final int numberOfSeats;
    private final String clientFile;
    private final String gameType;

    public GameClient(final String clientId,
                      final Integer numberOfSeats,
                      final String clientFile,
                      final String gameType,
                      final Map<String, String> clientProperties) {
        this.clientId = clientId;
        this.numberOfSeats = numberOfSeats;
        this.clientFile = clientFile;
        this.gameType = gameType;

        if (clientProperties != null) {
            this.clientProperties.putAll(clientProperties);
        }
    }

    public String getClientId() {
        return clientId;
    }

    public int getNumberOfSeats() {
        return numberOfSeats;
    }

    public String getClientFile() {
        return clientFile;
    }

    public Map<String, String> getClientProperties() {
        return clientProperties;
    }

    public String getGameType() {
        return gameType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final GameClient rhs = (GameClient) obj;
        return new EqualsBuilder()
                .append(clientFile, rhs.clientFile)
                .append(clientId, rhs.clientId)
                .append(clientProperties, rhs.clientProperties)
                .append(gameType, rhs.gameType)
                .append(numberOfSeats, rhs.numberOfSeats)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(clientFile)
                .append(clientId)
                .append(clientProperties)
                .append(gameType)
                .append(numberOfSeats)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(clientFile)
                .append(clientId)
                .append(clientProperties)
                .append(gameType)
                .append(numberOfSeats)
                .toString();
    }
}
