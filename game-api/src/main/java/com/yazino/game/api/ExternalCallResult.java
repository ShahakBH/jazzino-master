package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class ExternalCallResult implements Serializable {
    private final String id;
    private final BigDecimal playerId;
    private final boolean success;
    private final String callName;
    private final Object callResult;

    public ExternalCallResult(String id, BigDecimal playerId, boolean success, String callName, Object callResult) {
        this.id = id;
        this.playerId = playerId;
        this.success = success;
        this.callName = callName;
        this.callResult = callResult;
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getCallResult() {
        return callResult;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getId() {
        return id;
    }

    public String getCallName() {
        return callName;
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
        ExternalCallResult rhs = (ExternalCallResult) obj;
        return new EqualsBuilder()
                .append(this.id, rhs.id)
                .append(this.playerId, rhs.playerId)
                .append(this.success, rhs.success)
                .append(this.callName, rhs.callName)
                .append(this.callResult, rhs.callResult)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(playerId)
                .append(success)
                .append(callName)
                .append(callResult)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("playerId", playerId)
                .append("success", success)
                .append("callName", callName)
                .append("callResult", callResult)
                .toString();
    }
}
