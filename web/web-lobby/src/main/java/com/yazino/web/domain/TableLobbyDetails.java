package com.yazino.web.domain;

import com.yazino.platform.table.TableSummary;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class TableLobbyDetails implements Serializable {
    private static final long serialVersionUID = 148854070427028707L;

    private BigDecimal tableId;
    private String clientId;
    private String clientFileName;
    private String gameType;
    private String tableName;
    private String templateName;

    public TableLobbyDetails() {
    }

    public TableLobbyDetails(final BigDecimal tableId,
                             final String clientId,
                             final String clientFileName,
                             final String gameType,
                             final String tableName,
                             final String templateName) {
        notNull(tableId, "Table ID may not be null");
        notBlank(clientId, "Client id  may not be blank");
        notBlank(clientFileName, "Client file name may not be blank");
        notBlank(gameType, "Game Type name may not be blank");
        notNull(tableName, "Table Name may not be null");
        notBlank(templateName, "Template name may not be blank");

        this.tableId = tableId;
        this.clientFileName = clientFileName;
        this.gameType = gameType;
        this.tableName = tableName;
        this.clientId = clientId;
        this.templateName = templateName;
    }

    public TableLobbyDetails(final TableSummary table) {
        tableId = table.getId();
        clientId = table.getClientId();
        clientFileName = table.getClientFile();
        tableName = table.getName();
        templateName = table.getTemplateName();

        if (table.getGameType() != null) {
            gameType = table.getGameType().getId();
        } else {
            gameType = null;
        }
    }

    public static <T> Map<T, TableLobbyDetails> transform(final Map<T, TableSummary> toTransform) {
        final Map<T, TableLobbyDetails> transformed = new HashMap<T, TableLobbyDetails>();

        if (toTransform != null) {
            for (Map.Entry<T, TableSummary> summaryEntry : toTransform.entrySet()) {
                TableLobbyDetails details = null;
                if (summaryEntry.getValue() != null) {
                    details = new TableLobbyDetails(summaryEntry.getValue());
                }
                transformed.put(summaryEntry.getKey(), details);
            }
        }

        return transformed;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getClientFileName() {
        return clientFileName;
    }

    public void setClientFileName(final String clientFileName) {
        this.clientFileName = clientFileName;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final TableLobbyDetails rhs = (TableLobbyDetails) obj;
        return new EqualsBuilder()
                .append(tableId, rhs.tableId)
                .append(clientId, rhs.clientId)
                .append(clientFileName, rhs.clientFileName)
                .append(gameType, rhs.gameType)
                .append(tableName, rhs.tableName)
                .append(templateName, rhs.templateName)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(31, 43)
                .append(tableId)
                .append(clientId)
                .append(clientFileName)
                .append(gameType)
                .append(tableName)
                .append(templateName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
