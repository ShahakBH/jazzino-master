package com.yazino.platform.model.table;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.TransactionResult;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class TransactionResultWrapper implements Serializable, IdentifiedTableRequest {
    private static final long serialVersionUID = -1890188183699126670L;

    private final TableRequestType requestType = TableRequestType.TRANSACTION_RESULT;

    private final Long gameId;
    private final BigDecimal tableId;
    private final TransactionResult transactionResult;
    private final String commandReference;

    private String requestId;
    private Date timestamp;

    /**
     * Create an object wrapping the given result.
     *
     * @param tableId           the ID of the table the result was generated from.
     * @param gameId            the ID of the game the result was generated from.
     * @param transactionResult the result.
     * @param commandReference
     */
    public TransactionResultWrapper(final BigDecimal tableId,
                                    final Long gameId,
                                    final TransactionResult transactionResult,
                                    final String commandReference) {
        this.tableId = tableId;
        this.gameId = gameId;
        this.transactionResult = transactionResult;
        this.commandReference = commandReference;
    }

    /**
     * Get the wrapped result.
     *
     * @return the result or null if none set.
     */
    public TransactionResult getTransactionResult() {
        return transactionResult;
    }

    /**
     * Get the ID of the associated game.
     *
     * @return the game ID.
     */
    public Long getGameId() {
        return gameId;
    }

    /**
     * Set the ID of the associated table.
     *
     * @return the table ID.
     */
    public BigDecimal getTableId() {
        return tableId;
    }

    /**
     * Get the request identifier.
     *
     * @return the identifier.
     */
    public String getRequestId() {
        return requestId;
    }

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Set the request identifier.
     *
     * @param requestId the identifier.
     */
    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public String getCommandReference() {
        return commandReference;
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
        final TransactionResultWrapper rhs = (TransactionResultWrapper) obj;
        return new EqualsBuilder()
                .append(gameId, rhs.gameId)
                .append(requestId, rhs.requestId)
                .append(transactionResult, rhs.transactionResult)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(gameId)
                .append(requestId)
                .append(BigDecimals.strip(tableId))
                .append(transactionResult)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(gameId)
                .append(requestId)
                .append(tableId)
                .append(transactionResult)
                .toString();
    }
}
