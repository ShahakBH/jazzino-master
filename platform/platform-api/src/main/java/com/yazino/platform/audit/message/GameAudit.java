package com.yazino.platform.audit.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class GameAudit implements Serializable {
    private static final long serialVersionUID = 2021514146881343124L;

    @JsonProperty("lbl")
    private String auditLabel;
    @JsonProperty("hst")
    private String hostname;
    @JsonProperty("ts")
    private Date timeStamp;
    @JsonProperty("tbl")
    private BigDecimal tableId;
    @JsonProperty("gm")
    private Long gameId;
    @JsonProperty("inc")
    private Long increment;
    @JsonProperty("obs")
    private String observableStatusXml;
    @JsonProperty("int")
    private String internalStatusXml;
    @JsonProperty("pids")
    private Set<BigDecimal> playerIds;

    public GameAudit() {
    }

    public GameAudit(final String auditLabel,
                     final String hostname,
                     final Date timeStamp,
                     final BigDecimal tableId,
                     final Long gameId,
                     final Long increment,
                     final String observableStatusXml,
                     final String internalStatusXml,
                     final Set<BigDecimal> playerIds) {
        notNull(auditLabel, "auditLabel may not be null");
        notNull(timeStamp, "timeStamp may not be null");

        this.auditLabel = auditLabel;
        this.hostname = hostname;
        this.timeStamp = timeStamp;
        this.tableId = tableId;
        this.gameId = gameId;
        this.increment = increment;
        this.observableStatusXml = observableStatusXml;
        this.internalStatusXml = internalStatusXml;
        this.playerIds = playerIds;
    }

    public String getAuditLabel() {
        return auditLabel;
    }

    public void setAuditLabel(final String auditLabel) {
        this.auditLabel = auditLabel;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(final Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(final Long gameId) {
        this.gameId = gameId;
    }

    public Long getIncrement() {
        return increment;
    }

    public void setIncrement(final Long increment) {
        this.increment = increment;
    }

    public String getObservableStatusXml() {
        return observableStatusXml;
    }

    public void setObservableStatusXml(final String observableStatusXml) {
        this.observableStatusXml = observableStatusXml;
    }

    public String getInternalStatusXml() {
        return internalStatusXml;
    }

    public void setInternalStatusXml(final String internalStatusXml) {
        this.internalStatusXml = internalStatusXml;
    }

    public Set<BigDecimal> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(final Set<BigDecimal> playerIds) {
        this.playerIds = playerIds;
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
        final GameAudit rhs = (GameAudit) obj;
        return new EqualsBuilder()
                .append(auditLabel, rhs.auditLabel)
                .append(hostname, rhs.hostname)
                .append(timeStamp, rhs.timeStamp)
                .append(gameId, rhs.gameId)
                .append(increment, rhs.increment)
                .append(observableStatusXml, rhs.observableStatusXml)
                .append(internalStatusXml, rhs.internalStatusXml)
                .append(playerIds, rhs.playerIds)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(auditLabel)
                .append(hostname)
                .append(timeStamp)
                .append(BigDecimals.strip(tableId))
                .append(gameId)
                .append(increment)
                .append(observableStatusXml)
                .append(internalStatusXml)
                .append(playerIds)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(auditLabel)
                .append(hostname)
                .append(timeStamp)
                .append(tableId)
                .append(gameId)
                .append(increment)
                .append(observableStatusXml)
                .append(internalStatusXml)
                .append(playerIds)
                .toString();
    }
}
