package com.yazino.platform.model.table;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public final class TableControlMessage implements Serializable, TableRequest {
    private static final long serialVersionUID = 1932105559557885560L;

    private final TableRequestType requestType = TableRequestType.CONTROL;

    private final BigDecimal tableId;
    private final TableControlMessageType messageType;

    public TableControlMessage(final BigDecimal tableId,
                               final TableControlMessageType commandType) {
        notNull(tableId, "Table ID may not be null");
        notNull(commandType, "Command Type may not be null");

        this.tableId = tableId;
        this.messageType = commandType;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public TableControlMessageType getMessageType() {
        return messageType;
    }


    public TableRequestType getRequestType() {
        return requestType;
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

        final TableControlMessage rhs = (TableControlMessage) obj;
        return new EqualsBuilder()
                .append(messageType, rhs.messageType)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(messageType)
                .append(BigDecimals.strip(tableId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
