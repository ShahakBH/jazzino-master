package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@SpaceClass
public final class TablePersistenceRequest implements Serializable {
    private static final long serialVersionUID = 2L;
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ERROR = "Error";

    private String status = STATUS_PENDING;
    private BigDecimal tableId;
    private String spaceId;

    public TablePersistenceRequest() {
    }

    /**
     * creates a persistence wrapper using just the table ID. this
     * table persistence wrapper is not loaded fully.
     */
    public TablePersistenceRequest(final BigDecimal tableId) {
        this.tableId = tableId;
    }


    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    @SpaceIndex
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
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
        final TablePersistenceRequest rhs = (TablePersistenceRequest) obj;
        return new EqualsBuilder()
                .append(status, rhs.status)
                .append(spaceId, rhs.spaceId)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(status)
                .append(BigDecimals.strip(tableId))
                .append(spaceId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(status)
                .append(tableId)
                .append(spaceId)
                .toString();
    }
}
