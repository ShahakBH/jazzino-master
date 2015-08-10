package com.yazino.web.domain.payment;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

public class RegisteredCardQueryResult {
    private final List<RegisteredCreditCardDetails> creditCardDetailList;

    public List<RegisteredCreditCardDetails> getCreditCardDetailList() {
        return creditCardDetailList;
    }

    RegisteredCardQueryResult(final List<RegisteredCreditCardDetails> creditCardDetailList) {
        this.creditCardDetailList = creditCardDetailList;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
