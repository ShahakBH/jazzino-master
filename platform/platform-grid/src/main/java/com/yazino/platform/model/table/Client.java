package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.google.common.base.Function;
import com.yazino.platform.table.GameClient;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Map;

@SpaceClass
public final class Client implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final GameClientTransformer GAME_CLIENT_TRANSFORMER = new GameClientTransformer();

    private String clientId;
    private Integer numberOfSeats;
    private String clientFile;
    private String gameType;
    private Map<String, String> clientProperties;

    public Client() {
        // for gs
    }

    public Client(final String clientId,
                  final Integer numberOfSeats,
                  final String clientFile,
                  final String gameType,
                  final Map<String, String> clientProperties) {
        super();
        this.clientId = clientId;
        this.numberOfSeats = numberOfSeats;
        this.clientFile = clientFile;
        this.gameType = gameType;
        this.clientProperties = clientProperties;
    }

    public Client(final String clientId) {
        this.clientId = clientId;
    }

    @SpaceId
    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public Integer getNumberOfSeats() {
        return numberOfSeats;
    }

    public void setNumberOfSeats(final Integer numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }

    public String getClientFile() {
        return clientFile;
    }

    public void setClientFile(final String clientFile) {
        this.clientFile = clientFile;
    }

    public Map<String, String> getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(final Map<String, String> clientProperties) {
        this.clientProperties = clientProperties;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public GameClient asGameClient() {
        return GAME_CLIENT_TRANSFORMER.apply(this);
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
        final Client rhs = (Client) obj;
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

    private static class GameClientTransformer implements Function<Client, GameClient> {
        @Override
        public GameClient apply(final Client input) {
            if (input == null) {
                return null;
            }
            return new GameClient(input.getClientId(), input.getNumberOfSeats(), input.getClientFile(),
                    input.getGameType(), input.getClientProperties());
        }
    }


}
