package com.yazino.platform.model.statistic;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.processor.PersistenceRequest;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class PlayerLevelsPersistenceRequest implements PersistenceRequest<BigDecimal> {
    private static final long serialVersionUID = -4313573756689100626L;

    private Status status = Status.PENDING;
    private BigDecimal objectId;
    private Integer selector;

    public PlayerLevelsPersistenceRequest() {
    }

    public PlayerLevelsPersistenceRequest(final BigDecimal objectId) {
        notNull(objectId, "playerId may not be null");

        this.objectId = objectId;
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getObjectId() {
        return objectId;
    }

    public void setObjectId(final BigDecimal objectId) {
        this.objectId = objectId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    @Override
    public Integer getSelector() {
        return null;
    }

    @Override
    public void setSelector(final Integer selector) {
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
        final PlayerLevelsPersistenceRequest rhs = (PlayerLevelsPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(objectId, rhs.objectId)
                .append(status, rhs.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
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
