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
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TableRequestWrapper implements Serializable {
    private static final long serialVersionUID = 5141890054465253425L;

    public static final int ROUTING_MODULUS = 20;

    private String requestID;
    private BigDecimal tableId;
    private Integer selector;
    private TableRequest tableRequest;

    public TableRequestWrapper() {
    }

    public TableRequestWrapper(final int selector) {
        this.selector = selector;
    }

    public TableRequestWrapper(final TableRequest tableRequest) {
        notNull(tableRequest, "Request may not be null");

        this.tableId = tableRequest.getTableId();
        this.tableRequest = tableRequest;

        this.selector = tableId.hashCode() % ROUTING_MODULUS;

        setRequestID(UUID.randomUUID().toString());
    }

    public TableRequestWrapper(final AttemptToCloseTableRequest attemptToCloseTableRequest) {
        this((TableRequest) attemptToCloseTableRequest);

        setRequestID(attemptToCloseTableRequest.getTableId().toPlainString());
    }

    public TableRequestWrapper(final ProcessTableRequest processTableRequest) {
        this((TableRequest) processTableRequest);

        setRequestID(processTableRequest.getTableId().toPlainString());
    }

    public void setTableRequest(final TableRequest tableRequest) {
        this.tableRequest = tableRequest;
    }

    public TableRequest getTableRequest() {
        return tableRequest;
    }

    @SpaceRouting
    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    @SpaceId
    public String getRequestID() {
        return requestID;
    }

    public TableRequestType getRequestType() {
        if (tableRequest != null) {
            return tableRequest.getRequestType();
        }
        return null;
    }

    public void setRequestType(final TableRequestType requestType) {
        // do nothing
    }

    public void setRequestID(final String requestID) {
        this.requestID = requestID;

        if (tableRequest != null && tableRequest instanceof IdentifiedTableRequest) {
            ((IdentifiedTableRequest) tableRequest).setRequestId(requestID);
        }
    }

    @SpaceIndex
    public Integer getSelector() {
        return selector;
    }

    public void setSelector(final Integer selector) {
        this.selector = selector;
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
        final TableRequestWrapper rhs = (TableRequestWrapper) obj;
        return new EqualsBuilder()
                .append(tableRequest, rhs.tableRequest)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(tableRequest)
                .append(BigDecimals.strip(tableId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tableRequest)
                .append(tableId)
                .toString();
    }

}
