package com.yazino.web.payment.creditcard;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PurchaseOutcomeMapperTest {
    private static final String DEFAULT_MESSAGE = "this is the default msg for unknown codes";
    private static final String MESSAGE_1 = "message one";

    private PurchaseOutcomeMapper underTest;

    @Before
    public void setup() {
        final Map<String, String> errorCodes = new HashMap<String, String>();
        errorCodes.put(PurchaseOutcome.AVS_CHECK_FAILED.name(), MESSAGE_1);

        underTest = new PurchaseOutcomeMapper(DEFAULT_MESSAGE, errorCodes);
    }

    @Test
    public void shouldReturnDefaultMessageForNullCode() {
        final String actualMessage = underTest.getErrorMessage(null);

        assertThat(actualMessage, is(equalTo(DEFAULT_MESSAGE)));
    }

    @Test
    public void shouldReturnDefaultMessageForNullMerchant() {
        final String actualMessage = underTest.getErrorMessage(null);

        assertThat(actualMessage, is(equalTo(DEFAULT_MESSAGE)));
    }

    @Test
    public void shouldReturnDefaultMessageForInvalidMerchant() {
        final String actualMessage = underTest.getErrorMessage(null);

        assertThat(actualMessage, is(equalTo(DEFAULT_MESSAGE)));
    }

    @Test
    public void shouldReturnDefaultMessageForUnknownCode() {
        final String actualMessage = underTest.getErrorMessage(PurchaseOutcome.UNKNOWN);

        assertThat(actualMessage, is(equalTo(DEFAULT_MESSAGE)));
    }

    @Test
    public void shouldReturnMessageForKnownCode() {
        final String actualMessage = underTest.getErrorMessage(PurchaseOutcome.AVS_CHECK_FAILED);

        assertThat(actualMessage, is(equalTo(MESSAGE_1)));
    }
}
