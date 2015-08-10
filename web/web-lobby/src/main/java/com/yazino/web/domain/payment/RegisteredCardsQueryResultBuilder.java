package com.yazino.web.domain.payment;

import java.util.ArrayList;
import java.util.List;

public class RegisteredCardsQueryResultBuilder {

    private final List<RegisteredCreditCardDetails> creditCardDetailList;

    public RegisteredCardsQueryResultBuilder() {
        this.creditCardDetailList = new ArrayList<>();
    }

    public RegisteredCardsQueryResultBuilder withCreditCard(final RegisteredCreditCardDetails creditCardDetails) {
        this.creditCardDetailList.add(creditCardDetails);
        return this;
    }

    public RegisteredCardQueryResult build() {
        return new RegisteredCardQueryResult(creditCardDetailList);
    }
}
