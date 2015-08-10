package com.yazino.platform.model.table;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class ProcessTableRequest implements Serializable, TableRequest {
    private static final long serialVersionUID = -5119935118767146034L;

    private final TableRequestType requestType = TableRequestType.PROCESS;

    private final BigDecimal tableId;

    public ProcessTableRequest(final BigDecimal tableId) {
        notNull(tableId, "Table ID");

        this.tableId = tableId;
    }

    public BigDecimal getTableId() {
        return tableId;
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
        final ProcessTableRequest rhs = (ProcessTableRequest) obj;
        return BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tableId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tableId)
                .toString();
    }
}
