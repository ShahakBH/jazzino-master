package com.yazino.platform.model.account;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.yazino.platform.account.ExternalTransaction;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

@SpaceClass
public class ExternalTransactionPersistenceRequest implements Serializable {
    private static final long serialVersionUID = 8872711798639125827L;

    private String spaceId;
    private ExternalTransaction externalTransaction;

    public ExternalTransactionPersistenceRequest() {
    }

    public ExternalTransactionPersistenceRequest(final ExternalTransaction externalTransaction) {
        this.externalTransaction = externalTransaction;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public ExternalTransaction getExternalTransaction() {
        return externalTransaction;
    }

    public void setExternalTransaction(final ExternalTransaction externalTransaction) {
        this.externalTransaction = externalTransaction;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ExternalTransactionPersistenceRequest that = (ExternalTransactionPersistenceRequest) o;
        return new EqualsBuilder()
                .append(externalTransaction, that.externalTransaction)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(121, 4537)
                .append(externalTransaction)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(externalTransaction)
                .toString();
    }
}
