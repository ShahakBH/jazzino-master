package com.yazino.platform.table;

import com.yazino.game.api.GameType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class TableSummary implements Serializable {
    private static final long serialVersionUID = 8674895568564876382L;

    private final BigDecimal id;
    private final String name;
    private final TableStatus status;
    private final GameType gameType;
    private final BigDecimal ownerId;
    private final String clientId;
    private final String clientFile;
    private final String templateName;
    private final String monitoringMessage;
    private final String gameTypeId;
    private final Set<BigDecimal> playersAtTable;
    private final Set<String> tags;

    public TableSummary(final BigDecimal id,
                        final String name,
                        final TableStatus status,
                        final String gameTypeId,
                        final GameType gameType,
                        final BigDecimal ownerId,
                        final String clientId,
                        final String clientFile,
                        final String templateName,
                        final String monitoringMessage,
                        final Set<BigDecimal> playersAtTable,
                        final Set<String> tags) {
        notNull(id, "id may not be null");

        this.playersAtTable = playersAtTable;
        this.id = id;
        this.name = name;
        this.status = status;
        this.gameTypeId = gameTypeId;
        this.gameType = gameType;
        this.ownerId = ownerId;
        this.clientId = clientId;
        this.clientFile = clientFile;
        this.templateName = templateName;
        this.monitoringMessage = monitoringMessage;
        this.tags = tags;
    }

    public BigDecimal getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TableStatus getStatus() {
        return status;
    }

    public String getGameTypeId() {
        return gameTypeId;
    }

    public GameType getGameType() {
        return gameType;
    }


    public BigDecimal getOwnerId() {
        return ownerId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientFile() {
        return clientFile;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getMonitoringMessage() {
        return monitoringMessage;
    }

    public int getNumberOfPlayers() {
        if (playersAtTable != null) {
            return playersAtTable.size();
        }
        return 0;
    }

    public Set<BigDecimal> getPlayersAtTable() {
        if (playersAtTable == null) {
            return Collections.emptySet();
        }
        return playersAtTable;
    }

    public Set<String> getTags() {
        if (tags == null) {
            return Collections.emptySet();
        }
        return tags;
    }

    public boolean isOpen() {
        return status != null && TableStatus.open.equals(status);
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
        final TableSummary rhs = (TableSummary) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(status, rhs.status)
                .append(gameTypeId, rhs.gameTypeId)
                .append(gameType, rhs.gameType)
                .append(ownerId, rhs.ownerId)
                .append(clientId, rhs.clientId)
                .append(clientFile, rhs.clientFile)
                .append(templateName, rhs.templateName)
                .append(monitoringMessage, rhs.monitoringMessage)
                .append(playersAtTable, rhs.playersAtTable)
                .append(tags, rhs.tags)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .append(name)
                .append(status)
                .append(gameTypeId)
                .append(gameType)
                .append(ownerId)
                .append(clientId)
                .append(clientFile)
                .append(templateName)
                .append(monitoringMessage)
                .append(playersAtTable)
                .append(tags)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(name)
                .append(status)
                .append(gameTypeId)
                .append(gameType)
                .append(ownerId)
                .append(clientId)
                .append(clientFile)
                .append(templateName)
                .append(monitoringMessage)
                .append(playersAtTable)
                .append(tags)
                .toString();
    }
}
