package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class TableInvite implements Serializable, Comparable<TableInvite> {
    private static final long serialVersionUID = 5018929079998990680L;

    private BigDecimal id;
    private BigDecimal playerId;
    private BigDecimal tableId;
    private DateTime inviteTime;
    private Boolean open;

    public TableInvite() {
    }

    public TableInvite(final BigDecimal playerId,
                       final BigDecimal tableId,
                       final DateTime inviteTime) {
        this(playerId, tableId, inviteTime, true);
    }

    public TableInvite(final BigDecimal playerId,
                       final BigDecimal tableId,
                       final DateTime inviteTime,
                       final Boolean open) {
        notNull(playerId, "playerId is null");
        notNull(tableId, "tableId is null");
        notNull(inviteTime, "inviteTime is null");
        this.playerId = playerId;
        this.tableId = tableId;
        this.inviteTime = inviteTime;
        this.open = open;
    }

    @SpaceId
    public BigDecimal getId() {
        return id;
    }

    public void setId(final BigDecimal id) {
        this.id = id;
    }

    @SpaceIndex
    public DateTime getInviteTime() {
        return inviteTime;
    }

    public void setInviteTime(final DateTime inviteTime) {
        notNull(inviteTime, "inviteTime must not be null");
        this.inviteTime = inviteTime;
    }

    @SpaceIndex
    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    @SpaceRouting
    @SpaceIndex
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        notNull(playerId, "playerId must not be null");
        this.playerId = playerId;
    }

    @SpaceIndex
    public Boolean isOpen() {
        return open;
    }

    public void setOpen(final Boolean open) {
        this.open = open;
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

        final TableInvite rhs = (TableInvite) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(inviteTime, rhs.inviteTime)
                .append(open, rhs.open)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(tableId))
                .append(inviteTime)
                .append(open)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int compareTo(final TableInvite otherTableInvite) {
        return otherTableInvite.getInviteTime().compareTo(this.getInviteTime());
    }

}
