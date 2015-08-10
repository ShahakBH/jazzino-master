package com.yazino.platform.event.message;

import com.yazino.platform.messaging.Message;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountEvent implements PlatformEvent {

    private static final long serialVersionUID = 1755981796182147814L;
    @JsonProperty("id")
    private BigDecimal accountId;
    @JsonProperty("bal")
    private BigDecimal balance;

    protected AccountEvent() {
    }

    public AccountEvent(final BigDecimal accountId, final BigDecimal balance) {
        notNull(accountId, "accountId may not be null");
        notNull(balance, "balance may not be null");
        this.accountId = accountId;
        this.balance = balance;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    private void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    private void setBalance(final BigDecimal balance) {
        this.balance = balance;
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
        final AccountEvent rhs = (AccountEvent) obj;
        return new EqualsBuilder()
                .append(balance, rhs.balance)
                .isEquals()
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(balance)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "AccountEvent{"
                + "accountId='" + accountId + '\''
                + ", balance='" + balance + '\''
                + '}';
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.ACCOUNT;
    }
}
