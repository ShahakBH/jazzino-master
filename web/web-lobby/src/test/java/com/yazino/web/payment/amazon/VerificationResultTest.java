package com.yazino.web.payment.amazon;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class VerificationResultTest {

    @Test
    public void shouldResolveFromStatusCode() {
        assertEquals(VerificationResult.VALID, VerificationResult.fromStatusCode(200));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldBreakForInvalidStatusCode() {
        VerificationResult.fromStatusCode(28);
    }

    @Test
    public void isValidReturnsTrueFor200() {
        assertTrue(VerificationResult.VALID.isValid());

        assertFalse(VerificationResult.INVALID_USER.isValid());
        assertFalse(VerificationResult.INVALID_TRANSACTION.isValid());
    }
}
