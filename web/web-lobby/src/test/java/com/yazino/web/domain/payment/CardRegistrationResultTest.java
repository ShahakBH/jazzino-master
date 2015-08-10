package com.yazino.web.domain.payment;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class CardRegistrationResultTest {

    private static final String CARD_ID = "cardId";
    private static final String CUSTOMER_ID = "aCustomerId";
    private static final String OBSCURED_CARD_NUMBER = "aCardNumber";
    private static final String CUSTOMER_NAME = "aCustomerNumber";
    private static final String MESSAGE_CODE = "aMessageCode";
    private static final String MESSAGE = "aMessage";

    @Test
    public void shouldResolveExpiryYearFromDate() {
        final CardRegistrationResult underTest = new CardRegistrationResult(CARD_ID, CUSTOMER_ID, OBSCURED_CARD_NUMBER, CUSTOMER_NAME, "092013", MESSAGE_CODE, MESSAGE);

        assertThat(underTest.getExpiryYear(), is(equalTo("2013")));
        assertThat(underTest.getExpiryMonth(), is(equalTo("09")));
    }

}
