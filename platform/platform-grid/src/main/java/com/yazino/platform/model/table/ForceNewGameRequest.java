package com.yazino.platform.model.table;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.PlayerAtTableInformation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public class ForceNewGameRequest implements Serializable, TableRequest {

    private static final long serialVersionUID = -5775663354544233778L;

    private final TableRequestType requestType = TableRequestType.FORCE_NEW_GAME;

    private final BigDecimal tableId;
    private final Collection<PlayerAtTableInformation> playersInformation;
    private final BigDecimal variationTemplateId;
    private final String clientId;
    private final Map<BigDecimal, BigDecimal> overriddenAccountIds;

    public ForceNewGameRequest(final BigDecimal tableId,
                               final Collection<PlayerAtTableInformation> playersInformation,
                               final BigDecimal variationTemplateId,
                               final String clientId,
                               final Map<BigDecimal, BigDecimal> overriddenAccountIds) {
        this.tableId = tableId;
        this.playersInformation = playersInformation;
        this.variationTemplateId = variationTemplateId;
        this.clientId = clientId;
        this.overriddenAccountIds = overriddenAccountIds;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public Collection<PlayerAtTableInformation> getPlayersInformation() {
        return playersInformation;
    }

    public BigDecimal getVariationTemplateId() {
        return variationTemplateId;
    }

    public String getClientId() {
        return clientId;
    }

    public Map<BigDecimal, BigDecimal> getOverriddenAccountIds() {
        return overriddenAccountIds;
    }


    public TableRequestType getRequestType() {
        return requestType;
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
        final ForceNewGameRequest rhs = (ForceNewGameRequest) obj;
        return new EqualsBuilder()
                .append(playersInformation, rhs.playersInformation)
                .append(variationTemplateId, rhs.variationTemplateId)
                .append(clientId, rhs.clientId)
                .append(overriddenAccountIds, rhs.overriddenAccountIds)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tableId))
                .append(playersInformation)
                .append(variationTemplateId)
                .append(clientId)
                .append(overriddenAccountIds)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tableId)
                .append(playersInformation)
                .append(variationTemplateId)
                .append(clientId)
                .append(overriddenAccountIds)
                .toString();
    }
}
