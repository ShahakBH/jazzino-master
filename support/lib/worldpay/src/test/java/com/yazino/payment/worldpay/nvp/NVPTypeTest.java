package com.yazino.payment.worldpay.nvp;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NVPTypeTest {

    @Test
    public void aNumericTypeAcceptsZeroToNineAndTheDecimalPoint() {
        assertThat(NVPType.NUMERIC.validate("0123456789.00"), is(true));
    }

    @Test
    public void aNumericTypeDoesNotAcceptCharacters() {
        assertThat(NVPType.NUMERIC.validate("abcdef"), is(false));
    }

    @Test
    public void aNumericTypeDoesNotAcceptWhitespace() {
        assertThat(NVPType.NUMERIC.validate(" \t"), is(false));
    }

    @Test
    public void anAlphaTypeDoesNotAcceptZeroToNineAndTheDecimalPoint() {
        assertThat(NVPType.ALPHA.validate("0123456789.00"), is(false));
    }

    @Test
    public void aNumericTypeAcceptsCharacters() {
        assertThat(NVPType.ALPHA.validate("abcdef"), is(true));
    }

    @Test
    public void aNumericTypeDoesAcceptsWhitespace() {
        assertThat(NVPType.ALPHA.validate(" \t"), is(true));
    }

    @Test
    public void anAlphanumericTypeAcceptsZeroToNineAndTheDecimalPoint() {
        assertThat(NVPType.ALPHANUMERIC.validate("0123456789.00"), is(true));
    }

    @Test
    public void anAlphanumericTypeAcceptsCharacters() {
        assertThat(NVPType.ALPHANUMERIC.validate("abcdef"), is(true));
    }

    @Test
    public void anAlphaumericTypeDoesAcceptsWhitespace() {
        assertThat(NVPType.ALPHANUMERIC.validate(" \t"), is(true));
    }

}
