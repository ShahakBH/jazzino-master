package com.yazino.platform.email;

import com.yazino.platform.email.message.EmailMessage;
import com.yazino.platform.email.message.EmailSendMessage;
import com.yazino.platform.email.message.EmailVerificationMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.verify;

public class QueuePublishingAsyncEmailServiceTest {
    private static final String SUBJECT = "aSubject";
    private static final String TEMPLATE = "aTemplate";
    private static final String RECIPIENT_1 = "recipient1@example.com";
    private static final String RECIPIENT_2 = "recipient2@example.com";
    private static final String SENDER = "from@your.mum";

    @Mock
    private QueuePublishingService<EmailMessage> publishingService;

    private QueuePublishingAsyncEmailService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new QueuePublishingAsyncEmailService(publishingService);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPublishingService() {
        new QueuePublishingAsyncEmailService(null);
    }

    @Test
    public void theServiceSendsAnEmailMessageToMultipleRecipients() {
        underTest.send(asList(RECIPIENT_1, RECIPIENT_2), SENDER,
                SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue"));

        verify(publishingService).send(new EmailSendMessage(asList(RECIPIENT_1, RECIPIENT_2),
                SENDER, SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue")));
    }

    @Test
    public void theServiceSendsAnEmailMessageToASingleRecipients() {
        underTest.send(RECIPIENT_1, SENDER, SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue"));

        verify(publishingService).send(new EmailSendMessage(singleton(RECIPIENT_1),
                SENDER, SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue")));
    }

    @Test(expected = NullPointerException.class)
    public void theServiceThrowsAnExceptionIfANullRecipientIsSupplied() {
        underTest.send((String) null, SENDER, SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void theServiceThrowsAnExceptionIfABlankRecipientIsSupplied() {
        underTest.send("     ", SENDER, SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue"));
    }

    @Test(expected = NullPointerException.class)
    public void theServiceThrowsAnExceptionIfANullSenderIsSupplied() {
        underTest.send(RECIPIENT_1, null, SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void theServiceThrowsAnExceptionIfABlankSenderIsSupplied() {
        underTest.send(RECIPIENT_1, "", SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue"));
    }

    @Test(expected = NullPointerException.class)
    public void theServiceThrowsAnExceptionIfANullRecipientListIsSupplied() {
        underTest.send((Set<String>) null, SENDER, SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void theServiceThrowsAnExceptionIfAnEmptyRecipientListIsSupplied() {
        underTest.send(Collections.<String>emptySet(), SENDER,
                SUBJECT, TEMPLATE, singletonMap("aKey", (Object) "aValue"));
    }

    @Test(expected = NullPointerException.class)
    public void theServiceThrowsAnExceptionIfANullVerificationEmailIsSupplied() {
        underTest.verifyAddress(null);
    }

    @Test
    public void verifyingAnEmailSendsAVerificationMessage() {
        underTest.verifyAddress("anEmailAddress");

        verify(publishingService).send(new EmailVerificationMessage("anEmailAddress"));
    }

}
