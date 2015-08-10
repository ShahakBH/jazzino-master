package com.yazino.platform.email;

import com.google.common.base.Optional;
import com.yazino.email.EmailValidationResolver;
import com.yazino.email.EmailValidator;
import com.yazino.email.EmailVerificationResult;
import com.yazino.email.EmailVerificationStatus;
import com.yazino.platform.email.persistence.JDBCEmailValidationDAO;
import com.yazino.platform.event.message.EmailValidationEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.google.common.base.Optional.fromNullable;
import static com.yazino.email.EmailVerificationStatus.UNKNOWN_TEMPORARY;
import static com.yazino.email.EmailVerificationStatus.VALID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailValidationServiceTest {
    @Mock
    private EmailValidationResolver emailValidationResolver;
    @Mock
    private JDBCEmailValidationDAO emailValidationDao;
    @Mock
    private EmailValidator emailValidator;

    @Mock
    private QueuePublishingService<EmailValidationEvent> emailValidationEventService;
    private EmailValidationService underTest;

    @Before
    public void setUp() {
        when(emailValidationResolver.isValid(aResultWithStatus(VALID))).thenReturn(true);

        underTest = new CachingEmailValidationService(emailValidationResolver, emailValidator, emailValidationDao,
                emailValidationEventService);
    }

    @Test
    public void anEmailThatIsNotInTheDatabaseIsValidated() {
        when(emailValidationDao.findByAddress("aTestEmail@yazino.com")).thenReturn(Optional.<EmailVerificationResult>absent());
        when(emailValidator.validate("aTestEmail@yazino.com")).thenReturn(aResultWithStatus(VALID));

        assertThat(underTest.validate("aTestEmail@yazino.com"), is(equalTo(true)));

        verify(emailValidationEventService).send(new EmailValidationEvent("aTestEmail@yazino.com", "V"));
        verify(emailValidationDao).save(new EmailVerificationResult("aTestEmail@yazino.com", EmailVerificationStatus.VALID));
        verify(emailValidator).validate("aTestEmail@yazino.com");
    }

    @Test
    public void anEmailWithoutATemporaryStatusThatAlreadyExistsInTheDatabaseIsNotValidatedAgain() {
        when(emailValidationDao.findByAddress("aTestEmail@yazino.com")).thenReturn(fromNullable(aResultWithStatus(VALID)));

        assertThat(underTest.validate("aTestEmail@yazino.com"), is(equalTo(true)));
        verify(emailValidationDao).findByAddress("aTestEmail@yazino.com");
        verifyZeroInteractions(emailValidationEventService);
        verifyNoMoreInteractions(emailValidationDao);
        verifyZeroInteractions(emailValidator);
    }

    @Test
    public void anEmailWithATemporaryStatusThatAlreadyExistsInTheDatabaseIsValidatedAgain() {
        when(emailValidationDao.findByAddress("aTestEmail@yazino.com")).thenReturn(fromNullable(aResultWithStatus(UNKNOWN_TEMPORARY)));
        when(emailValidator.validate("aTestEmail@yazino.com")).thenReturn(aResultWithStatus(VALID));

        assertThat(underTest.validate("aTestEmail@yazino.com"), is(equalTo(true)));

        verify(emailValidationEventService).send(new EmailValidationEvent("aTestEmail@yazino.com", "V"));
        verify(emailValidationDao).save(new EmailVerificationResult("aTestEmail@yazino.com", EmailVerificationStatus.VALID));
        verify(emailValidator).validate("aTestEmail@yazino.com");
    }

    private EmailVerificationResult aResultWithStatus(final EmailVerificationStatus status) {
        return new EmailVerificationResult("aTestEmail@yazino.com", status);
    }

}
