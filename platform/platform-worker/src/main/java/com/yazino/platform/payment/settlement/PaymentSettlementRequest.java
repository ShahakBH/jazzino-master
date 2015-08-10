package com.yazino.platform.payment.settlement;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class PaymentSettlementRequest implements Message, Serializable {
    private static final long serialVersionUID = -8717810471586931810L;

    @JsonProperty("intTx")
    private String internalTransactionId;

    public PaymentSettlementRequest() {
    }

    public PaymentSettlementRequest(final String internalTransactionId) {
        this.internalTransactionId = internalTransactionId;
    }

    public void setInternalTransactionId(final String internalTransactionId) {
        this.internalTransactionId = internalTransactionId;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Object getMessageType() {
        return "PaymentSettlementRequest";
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
        PaymentSettlementRequest rhs = (PaymentSettlementRequest) obj;
        return new EqualsBuilder()
                .append(this.internalTransactionId, rhs.internalTransactionId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(internalTransactionId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("internalTransactionId", internalTransactionId)
                .toString();
    }
}
