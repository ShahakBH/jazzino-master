package com.yazino.platform.model.account;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.processor.PersistenceRequest;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class AccountPersistenceRequest implements PersistenceRequest<BigDecimal> {
    private static final long serialVersionUID = 8136553810049815502L;

    private Status status = Status.PENDING;
    private BigDecimal objectId;
    private Integer selector;

    public AccountPersistenceRequest() {
    }

    public AccountPersistenceRequest(final BigDecimal objectId) {
        notNull(objectId, "objectId may not be null");

        this.objectId = objectId;
        this.selector = objectId.hashCode() % ROUTING_MODULUS;
    }

    @SpaceRouting
    @SpaceId
    public BigDecimal getObjectId() {
        return objectId;
    }

    public void setObjectId(final BigDecimal accountId) {
        this.objectId = accountId;
    }

    @SpaceIndex
    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    @Override
    public Integer getSelector() {
        return selector;
    }

    @Override
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
        final AccountPersistenceRequest rhs = (AccountPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(objectId, rhs.objectId)
                .append(status, rhs.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(objectId)
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(objectId)
                .append(status)
                .toString();
    }
}
