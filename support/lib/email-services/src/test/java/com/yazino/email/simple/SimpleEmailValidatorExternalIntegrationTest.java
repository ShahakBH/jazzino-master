package com.yazino.email.simple;

import com.yazino.email.EmailVerificationResult;
import org.junit.Before;
import org.junit.Test;

import static com.yazino.email.EmailVerificationStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This verifies a subset of behaviour against external SMTP servers. As such, it's likely to prove fragile.
 */
public class SimpleEmailValidatorExternalIntegrationTest {

    private SimpleEmailValidator underTest;

    @Before
    public void setUp() {
        underTest = new SimpleEmailValidator();
    }

    @Test
    public void aValidGmailAddressIsAccepted() {
        assertThat(underTest.validate("hchahine@yazino.com"),
                is(equalTo(new EmailVerificationResult("hchahine@yazino.com", VALID))));
    }

    @Test
    public void aInvalidGmailAddressIsNotAccepted() {
        assertThat(underTest.validate("staff.invalid@yazino.com"),
                is(equalTo(new EmailVerificationResult("staff.invalid@yazino.com", INVALID))));
    }

    @Test
    public void aMalformedEmailAddressWithNoHostPartIsNotAccepted() {
        assertThat(underTest.validate("an.email@"),
                is(equalTo(new EmailVerificationResult("an.email@", MALFORMED))));
    }

    @Test
    public void aMalformedEmailAddressWithNoSeparatorIsNotAccepted() {
        assertThat(underTest.validate("an.emailhost"),
                is(equalTo(new EmailVerificationResult("an.emailhost", MALFORMED))));
    }

    @Test
    public void anEmailAddressWithAnInvalidHostnameIsNotAccepted() {
        assertThat(underTest.validate("an.email@example"),
                is(equalTo(new EmailVerificationResult("an.email@example", INVALID))));
    }

    @Test
    public void anEmailAddressWithAServerThatTimesOutSmtpServerIsAccepted() {
        assertThat(underTest.validate("an.email@example.com"),
                is(equalTo(new EmailVerificationResult("an.email@example.com", UNKNOWN_TEMPORARY))));
    }

    @Test
    public void aValidAddressWithNoMXRecordsAndAValidARecordIsAccepted() {
        underTest.setSocketTimeout(10000); // Mac OS X Server seems to hang for a bit on EHLO
        assertThat(underTest.validate("admin@deus.london.yazino.com"),
                is(equalTo(new EmailVerificationResult("admin@deus.london.yazino.com", VALID))));
    }

    @Test
    public void anInvalidAddressWithADodgySmtpServerIsNotAccepted() {
        assertThat(underTest.validate("xvdhgd@eusu.com"),
                is(equalTo(new EmailVerificationResult("xvdhgd@eusu.com", INVALID))));
    }
}
