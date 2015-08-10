package com.yazino.email;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.yazino.email.EmailVerificationStatus.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailValidationResolverTest {
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private EmailValidationResolver underTest;

    @Before
    public void setUp() {
        when(yazinoConfiguration.getBoolean("email.allow-unknown", true)).thenReturn(true);
        when(yazinoConfiguration.getBoolean("email.allow-accept-all", true)).thenReturn(true);
        when(yazinoConfiguration.getBoolean("email.allow-service-error", true)).thenReturn(true);
        when(yazinoConfiguration.getBoolean("email.allow-disposable", false)).thenReturn(false);
        when(yazinoConfiguration.getBoolean("email.allow-role", false)).thenReturn(false);

        underTest = new EmailValidationResolver(yazinoConfiguration);
    }

    @Test
    public void validationFailsWhenTheResultIsTemporarilyUnknownAndAllowServiceErrorIsFalse() {
        when(yazinoConfiguration.getBoolean("email.allow-service-error", true)).thenReturn(false);

        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", UNKNOWN_TEMPORARY)), is(false));
    }

    @Test
    public void validationSucceedsWhenTheResultIsTemporarilyUnknownAndAllowServiceErrorIsTrue() {
        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", UNKNOWN_TEMPORARY)), is(true));
    }

    @Test
    public void validationFailsWhenTheResultIsAcceptsAllAndAllowAcceptsAllIsFalse() {
        when(yazinoConfiguration.getBoolean("email.allow-accept-all", true)).thenReturn(false);

        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", ACCEPT_ALL)), is(false));
    }

    @Test
    public void validationSucceedsWhenTheResultIsAcceptAllAndAllowAcceptAllIsTrue() {
        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", ACCEPT_ALL)), is(true));
    }

    @Test
    public void validationFailsWhenTheResultIsUnknownAndAllowUnknownIsFalse() {
        when(yazinoConfiguration.getBoolean("email.allow-unknown", true)).thenReturn(false);

        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", UNKNOWN)), is(false));
    }

    @Test
    public void validationSucceedsWhenTheResultIsUnknownAndAllowUnknownIsTrue() {
        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", UNKNOWN)), is(true));
    }

    @Test
    public void validationSucceedsWhenTheResultIsValid() {
        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", VALID)), is(true));
    }

    @Test
    public void validationFailsWhenTheResultIsInvalid() {
        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", INVALID)), is(false));
    }

    @Test
    public void validationOfADisposableEmailFailsWhenAllowDisposableIsFalse() {
        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", VALID, true, false)), is(false));
    }

    @Test
    public void validationOfADisposableEmailPassesWhenAllowDisposableIsTrue() {
        when(yazinoConfiguration.getBoolean("email.allow-disposable", false)).thenReturn(true);

        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", VALID, true, false)), is(true));
    }

    @Test
    public void validationOfARoleEmailFailsWhenAllowRoleIsFalse() {
        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", VALID, false, true)), is(false));
    }

    @Test
    public void validationOfARoleEmailPassesWhenAllowRoleIsTrue() {
        when(yazinoConfiguration.getBoolean("email.allow-role", false)).thenReturn(true);

        assertThat(underTest.isValid(new EmailVerificationResult("anAddress", VALID, false, true)), is(true));
    }

}
