package com.yazino.platform.audit.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notNull;

public class CommandAudit implements Serializable {
    private static final long serialVersionUID = -2646077680447724138L;

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
    @JsonProperty("tp")
    private String type;
    @JsonProperty("arg")
    private String[] args;
    @JsonProperty("plId")
    private BigDecimal playerId;
    @JsonProperty("id")
    private String uuid;

    public CommandAudit() {
    }

    public CommandAudit(final String auditLabel,
                        final String hostname,
                        final Date timeStamp,
                        final BigDecimal tableId,
                        final Long gameId,
                        final String type,
                        final String[] args,
                        final BigDecimal playerId,
                        final String uuid) {
        notNull(auditLabel, "auditLabel may not be null");
        notNull(timeStamp, "timeStamp may not be null");

        this.auditLabel = auditLabel;
        this.hostname = hostname;
        this.timeStamp = timeStamp;
        this.tableId = tableId;
        this.gameId = gameId;
        this.type = type;
        this.args = args;
        this.playerId = playerId;
        this.uuid = uuid;
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

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(final String[] args) {
        this.args = args;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
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
        final CommandAudit rhs = (CommandAudit) obj;
        return new EqualsBuilder()
                .append(auditLabel, rhs.auditLabel)
                .append(hostname, rhs.hostname)
                .append(timeStamp, rhs.timeStamp)
                .append(gameId, rhs.gameId)
                .append(type, rhs.type)
                .append(args, rhs.args)
                .append(uuid, rhs.uuid)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
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
                .append(type)
                .append(args)
                .append(BigDecimals.strip(playerId))
                .append(uuid)
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
                .append(type)
                .append(args)
                .append(playerId)
                .append(uuid)
                .toString();
    }
}
