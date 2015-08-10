package com.yazino.platform.email.consumer;

import com.yazino.email.EmailException;
import com.yazino.email.EmailService;
import com.yazino.platform.email.EmailValidationService;
import com.yazino.platform.email.message.EmailSendMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailMessageConsumerTest {

    @Mock
    private EmailService emailService;
    @Mock
    private EmailValidationService emailValidationService;

    private EmailMessageConsumer underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new EmailMessageConsumer(emailService, emailValidationService);
    }

    @Test(expected = NullPointerException.class)
    public void theConsumerCannotBeCreatedWithANullService() {
        new EmailMessageConsumer(null, emailValidationService);
    }

    @Test(expected = NullPointerException.class)
    public void theConsumerCannotBeCreatedWithANullValidationService() {
        new EmailMessageConsumer(emailService, null);
    }

    @Test
    public void nullMessagesAreNotProcessed() {
        underTest.handle(null);

        verifyZeroInteractions(emailValidationService, emailService);
    }

    @Test
    public void emailMessagesWithAnUnknownVersionAreNotProcessed() {
        final EmailSendMessage emailSendMessage = mock(EmailSendMessage.class);
        when(emailSendMessage.getVersion()).thenReturn(2);

        underTest.handle(emailSendMessage);

        verifyZeroInteractions(emailValidationService, emailService);
    }

    @Test
    public void emailsAreSentToAddressesThatPassValidation() throws EmailException {
        when(emailValidationService.validate(anyString())).thenReturn(true);

        underTest.handle(aMessage());

        verify(emailService).send(new String[]{"recipient1@example.com", "recipient2@example.com"},
                "from@your.mum", "aSubject", "aTemplate", singletonMap("aKey", (Object) "aValue"));
    }

    @Test
    public void emailsAreNotSentToAddressesThatFailValidation() throws EmailException {
        when(emailValidationService.validate("recipient1@example.com")).thenReturn(false);
        when(emailValidationService.validate("recipient2@example.com")).thenReturn(true);

        underTest.handle(aMessage());

        verify(emailService).send(new String[]{"recipient2@example.com"},
                "from@your.mum", "aSubject", "aTemplate", singletonMap("aKey", (Object) "aValue"));
    }

    @Test
    public void emailsAreNotSentIfAllAddressesFailValidation() throws EmailException {
        when(emailValidationService.validate(anyString())).thenReturn(false);

        underTest.handle(aMessage());

        verifyZeroInteractions(emailService);
    }

    @Test
    public void exceptionsFromTheValidatorAreNotPropagated() throws EmailException {
        when(emailValidationService.validate(anyString())).thenThrow(new RuntimeException("aTestException"));

        underTest.handle(aMessage());

        verifyZeroInteractions(emailService);
    }

    @Test
    public void exceptionsFromTheEmailServiceAreNotPropagated() throws EmailException {
        when(emailValidationService.validate(anyString())).thenReturn(true);
        doThrow(new EmailException("aTestException")).when(emailService).send(new String[]{"recipient2@example.com"},
                "from@your.mum", "aSubject", "aTemplate", singletonMap("aKey", (Object) "aValue"));

        underTest.handle(aMessage());
    }

    private EmailSendMessage aMessage() {
        return new EmailSendMessage(asList("recipient1@example.com", "recipient2@example.com"),
                "from@your.mum", "aSubject", "aTemplate", singletonMap("aKey", (Object) "aValue"));
    }

}
