package com.yazino.platform.model.account;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class CloseAccountRequest implements Serializable {
    private static final long serialVersionUID = -5703986189435731710L;
    private String requestId;
    private BigDecimal accountId;

    public CloseAccountRequest() {
    }

    public CloseAccountRequest(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    @SpaceId(autoGenerate = true)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    @SpaceRouting
    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
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
        final CloseAccountRequest rhs = (CloseAccountRequest) obj;
        return new EqualsBuilder()
                .append(requestId, rhs.requestId)
                .isEquals()
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(requestId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(accountId)
                .append(requestId)
                .toString();
    }
}
