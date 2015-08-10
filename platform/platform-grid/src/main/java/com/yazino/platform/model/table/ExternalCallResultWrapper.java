package com.yazino.platform.model.table;

import com.yazino.game.api.ExternalCallResult;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

public class ExternalCallResultWrapper implements TableRequest, IdentifiedTableRequest {

    private BigDecimal tableId;
    private final ExternalCallResult externalCallResult;
    private String requestId;

    public ExternalCallResultWrapper(BigDecimal tableId, ExternalCallResult externalCallResult) {
        this.tableId = tableId;
        this.externalCallResult = externalCallResult;
    }

    public void setTableId(BigDecimal tableId) {
        this.tableId = tableId;
    }

    public ExternalCallResult getExternalCallResult() {
        return externalCallResult;
    }

    @Override
    public BigDecimal getTableId() {
        return tableId;
    }

    @Override
    public TableRequestType getRequestType() {
        return TableRequestType.EXTERNAL_CALL_RESULT;
    }

    @Override
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        ExternalCallResultWrapper rhs = (ExternalCallResultWrapper) obj;
        return new EqualsBuilder()
                .append(this.tableId, rhs.tableId)
                .append(this.externalCallResult, rhs.externalCallResult)
                .append(this.requestId, rhs.requestId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(tableId)
                .append(externalCallResult)
                .append(requestId)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("tableId", tableId)
                .append("externalCallResult", externalCallResult)
                .append("requestId", requestId)
                .toString();
    }
}
