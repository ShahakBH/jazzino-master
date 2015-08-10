package com.yazino.platform.gamehost.external;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@SpaceClass
public class NovomaticRequest implements Serializable {
    private String requestId;
    private BigDecimal tableId;
    private BigDecimal playerId;
    private String callName;
    private Object callContext;

    //cglib
    public NovomaticRequest() {
    }

    public NovomaticRequest(String requestId, BigDecimal tableId, BigDecimal playerId,
                            String callName, Object callContext) {
        this.requestId = requestId;
        this.tableId = tableId;
        this.playerId = playerId;
        this.callName = callName;
        this.callContext = callContext;
    }


    @SpaceId
    public String getRequestId() {
        return requestId;
    }

    @SpaceRouting
    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(BigDecimal tableId) {
        this.tableId = tableId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getCallName() {
        return callName;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setCallName(String callName) {
        this.callName = callName;
    }

    public Object getCallContext() {
        return callContext;
    }

    public void setCallContext(Object callContext) {
        this.callContext = callContext;
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
        NovomaticRequest rhs = (NovomaticRequest) obj;
        return new EqualsBuilder()
                .append(this.requestId, rhs.requestId)
                .append(this.tableId, rhs.tableId)
                .append(this.playerId, rhs.playerId)
                .append(this.callName, rhs.callName)
                .append(this.callContext, rhs.callContext)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(requestId)
                .append(tableId)
                .append(playerId)
                .append(callName)
                .append(callContext)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("requestId", requestId)
                .append("tableId", tableId)
                .append("playerId", playerId)
                .append("callName", callName)
                .append("callContext", callContext)
                .toString();
    }
}
