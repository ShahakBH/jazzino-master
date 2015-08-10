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
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TableTransactionRequest implements Serializable {
    private static final long serialVersionUID = 4394727309884172460L;
    private String requestID;
    private BigDecimal tableId;
    private List<PostTransactionAtTable> transactions;
    private Set<BigDecimal> releaseRequests;
    private String auditLabel;
    private Date timestamp;

    public TableTransactionRequest() {
    }

    public TableTransactionRequest(final BigDecimal tableId,
                                   final List<PostTransactionAtTable> transactions,
                                   final Set<BigDecimal> releaseRequests,
                                   final String auditLabel) {
        notNull(tableId, "tableId is null");
        notNull(transactions, "transactions is null");
        notNull(releaseRequests, "releaseRequests is null");

        this.tableId = tableId;
        this.transactions = new ArrayList<PostTransactionAtTable>(transactions);
        this.releaseRequests = new HashSet<BigDecimal>(releaseRequests);
        this.auditLabel = auditLabel;
    }

    public void setRequestID(final String requestID) {
        this.requestID = requestID;
    }

    @SpaceId(autoGenerate = true)
    public String getRequestID() {
        return requestID;
    }

    @SpaceRouting
    @SpaceIndex
    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public Set<BigDecimal> getReleaseRequests() {
        return releaseRequests;
    }

    public void setReleaseRequests(final Set<BigDecimal> releaseRequests) {
        this.releaseRequests = releaseRequests;
    }

    public List<PostTransactionAtTable> getTransactions() {
        return transactions;
    }

    public void setTransactions(final List<PostTransactionAtTable> transactions) {
        this.transactions = transactions;
    }

    public String getAuditLabel() {
        return auditLabel;
    }

    public void setAuditLabel(final String auditLabel) {
        this.auditLabel = auditLabel;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
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
        final TableTransactionRequest rhs = (TableTransactionRequest) obj;
        return new EqualsBuilder()
                .append(requestID, rhs.requestID)
                .append(auditLabel, rhs.auditLabel)
                .append(transactions, rhs.transactions)
                .append(releaseRequests, rhs.releaseRequests)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(requestID)
                .append(BigDecimals.strip(tableId))
                .append(auditLabel)
                .append(transactions)
                .append(releaseRequests)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(requestID)
                .append(tableId)
                .append(auditLabel)
                .append(transactions)
                .append(releaseRequests)
                .toString();
    }
}
