package com.yazino.platform.community;

import com.yazino.platform.table.TableSummary;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TableInviteSummary implements Serializable {

    private static final long serialVersionUID = 2513066008039277846L;

    private String tableName;
    private BigDecimal tableId;
    private String variationName;
    private String gameTitle;
    private String invitorName;
    private String gameType;
    private String invitorPictureUrl;

    public TableInviteSummary(final TableSummary table,
                              final String ownerName,
                              final String ownerPictureUrl) {
        notNull(table, "table is null");
        notNull(ownerName, "ownerName is null");

        tableName = table.getName();
        tableId = table.getId();
        variationName = table.getTemplateName();
        gameTitle = table.getGameType().getName();
        gameType = table.getGameType().getId();
        invitorName = ownerName;
        invitorPictureUrl = ownerPictureUrl;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public void setGameTitle(final String gameTitle) {
        this.gameTitle = gameTitle;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public String getInvitorName() {
        return invitorName;
    }

    public void setInvitorName(final String invitorName) {
        this.invitorName = invitorName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    public String getVariationName() {
        return variationName;
    }

    public void setVariationName(final String variationName) {
        this.variationName = variationName;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getInvitorPictureUrl() {
        return invitorPictureUrl;
    }

    public void setInvitorPictureUrl(final String invitorPictureUrl) {
        this.invitorPictureUrl = invitorPictureUrl;
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

        final TableInviteSummary rhs = (TableInviteSummary) obj;
        return new EqualsBuilder()
                .append(tableName, rhs.tableName)
                .append(variationName, rhs.variationName)
                .append(gameTitle, rhs.gameTitle)
                .append(invitorName, rhs.invitorName)
                .append(gameType, rhs.gameType)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tableId))
                .append(tableName)
                .append(variationName)
                .append(gameTitle)
                .append(invitorName)
                .append(gameType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tableId)
                .append(tableName)
                .append(variationName)
                .append(gameTitle)
                .append(invitorName)
                .append(gameType)
                .toString();
    }
}
