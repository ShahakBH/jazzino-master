package com.yazino.email.briteverify;

import com.yazino.email.EmailVerificationResult;
import com.yazino.email.EmailVerificationStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.yazino.email.EmailVerificationStatus.INVALID;
import static com.yazino.email.EmailVerificationStatus.VALID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class BriteVerifyEmailValidatorExternalIntegrationTest {

    @Autowired
    private BriteVerifyEmailValidator underTest;

    @Test
    public void aValidEmailAddressPassesValidation() {
        assertThat(underTest.validate("staff@yazino.com"), is(equalTo(new EmailVerificationResult("staff@yazino.com", VALID))));
    }

    @Test
    public void anInvalidEmailAddressFailsValidation() {
        assertThat(underTest.validate("fgwnjk345bjhdfzbgjhzcfbjhfzb@yazino.com"),
                is(equalTo(new EmailVerificationResult("fgwnjk345bjhdfzbgjhzcfbjhfzb@yazino.com", INVALID))));
    }

}
