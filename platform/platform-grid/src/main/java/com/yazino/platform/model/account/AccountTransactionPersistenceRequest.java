package com.yazino.platform.model.account;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.gigaspaces.metadata.index.SpaceIndexType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class AccountTransactionPersistenceRequest implements Serializable {
    private static final long serialVersionUID = -6737693288762485523L;

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ERROR = "Error";

    private String status = STATUS_PENDING;
    private AccountTransaction accountTransaction;
    private String spaceId;

    public AccountTransactionPersistenceRequest() {
    }

    public AccountTransactionPersistenceRequest(final AccountTransaction accountTransaction) {
        this.accountTransaction = accountTransaction;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    @SpaceRouting
    public BigDecimal getAccountId() {
        if (accountTransaction != null) {
            return accountTransaction.getAccountId();
        }
        return null;
    }

    public void setAccountId(final BigDecimal accountId) {
        // required by GS
    }

    public AccountTransaction getAccountTransaction() {
        return accountTransaction;
    }

    public void setAccountTransaction(final AccountTransaction accountTransaction) {
        this.accountTransaction = accountTransaction;
    }

    @SpaceIndex(type = SpaceIndexType.BASIC)
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
        final AccountTransactionPersistenceRequest rhs = (AccountTransactionPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(status, rhs.status)
                .append(accountTransaction, rhs.accountTransaction)
                .append(spaceId, rhs.spaceId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(status)
                .append(accountTransaction)
                .append(spaceId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(status)
                .append(accountTransaction)
                .append(spaceId)
                .toString();
    }
}
