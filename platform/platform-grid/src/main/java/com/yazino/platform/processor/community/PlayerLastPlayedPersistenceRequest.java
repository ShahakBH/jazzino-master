package com.yazino.platform.processor.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.processor.PersistenceRequest;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class PlayerLastPlayedPersistenceRequest implements PersistenceRequest<BigDecimal> {

    private static final long serialVersionUID = -4313573756689100626L;

    private Status status = Status.PENDING;
    private BigDecimal objectId;
    private Integer selector;

    public PlayerLastPlayedPersistenceRequest() {
    }

    public PlayerLastPlayedPersistenceRequest(final BigDecimal objectId) {
        notNull(objectId, "playerId may not be null");
        this.objectId = objectId;
        status = Status.PENDING;
    }

    @SpaceId
    @SpaceRouting
    @Override
    public BigDecimal getObjectId() {
        return objectId;
    }

    @Override
    public void setObjectId(final BigDecimal id) {
        this.objectId = id;
    }

    @SpaceIndex
    @Override
    public Status getStatus() {
        return status;
    }

    @Override
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
        PlayerLastPlayedPersistenceRequest rhs = (PlayerLastPlayedPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(this.status, rhs.status)
                .append(this.objectId, rhs.objectId)
                .append(this.selector, rhs.selector)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(status)
                .append(objectId)
                .append(selector)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "PlayerLastPlayedPersistenceRequest{"
                + "status=" + status
                + ", objectId=" + objectId
                + ", selector=" + selector
                + '}';
    }
}
