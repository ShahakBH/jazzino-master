package com.yazino.email.simple;

import com.yazino.email.EmailException;
import com.yazino.email.EmailService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SimpleEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private MimeMessage mimeMessage;
    @Mock
    private com.yazino.configuration.YazinoConfiguration yazinoConfiguration;

    private SimpleEmailService unit;

    private static final String TO_EXAMPLE = "to@example.com";
    private static final String SUBJECT_DEFAULT = "Test 1";
    private static final String FROM_ADDRESS_DEFAULT = "test@example.com";
    private static final String CONTENT_DEFAULT = "<b>Hello, world</b>\n";

    @Before
    public void setUp() {
        when(yazinoConfiguration.getBoolean(EmailService.PROPERTY_EMAIL_ENABLED, true)).thenReturn(true);

        unit = new SimpleEmailService(mailSender, yazinoConfiguration);
    }

    @Test
    public void shouldSendMessageWhenEmailEnabledPropertyIsPresentAndTrue() throws EmailException, MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        final Map<String, Object> templateProperties = new HashMap<String, Object>();
        templateProperties.put("subject", "world");

        unit.send(TO_EXAMPLE, FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "demo", templateProperties);

        verify(mailSender).send(mimeMessage);
        verifyExpectationsForMimeMessage(FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, CONTENT_DEFAULT, TO_EXAMPLE);
    }


    @Test
    public void shouldNotSendMessageWhenEmailEnabledPropertyIsPresentAndFalse() throws EmailException, MessagingException {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getBoolean(EmailService.PROPERTY_EMAIL_ENABLED, true)).thenReturn(false);
        final Map<String, Object> templateProperties = new HashMap<String, Object>();
        templateProperties.put("subject", "world");

        unit.send(TO_EXAMPLE, FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "demo", templateProperties);

        verifyZeroInteractions(mailSender);
    }

    @Test
    public void shouldSendMessageWithNoSubject() throws EmailException, MessagingException {

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        final Map<String, Object> templateProperties = new HashMap<String, Object>();
        templateProperties.put("subject", "world");

        unit.send(TO_EXAMPLE, FROM_ADDRESS_DEFAULT, null, "demo", templateProperties);

        verify(mailSender).send(mimeMessage);
        verifyExpectationsForMimeMessage(FROM_ADDRESS_DEFAULT, null, CONTENT_DEFAULT, TO_EXAMPLE);
    }

    @Test
    public void shouldSendMessagesToMultipleRecipients() throws MessagingException, EmailException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String otherTo = "other@example.com";
        final Map<String, Object> templateProperties = new HashMap<String, Object>();
        templateProperties.put("subject", "world");

        unit.send(new String[]{TO_EXAMPLE, otherTo}, FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "demo", templateProperties);

        verify(mailSender).send(mimeMessage);
        verifyExpectationsForMimeMessage(FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, CONTENT_DEFAULT, TO_EXAMPLE, otherTo);
    }

    @Test
    public void shouldSendMessageWithNullProperties() throws EmailException, MessagingException {

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        unit.send(TO_EXAMPLE, FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "demo", null);

        verify(mailSender).send(mimeMessage);
        verifyExpectationsForMimeMessage(FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "<b>Hello, $subject</b>\n", TO_EXAMPLE);
    }

    @Test
    public void shouldSendMessageWithFileSystemTemplate() throws EmailException, MessagingException, IOException {
        final File tempFile = File.createTempFile("dummy", "tmp");
        final File tempDir = tempFile.getParentFile();
        tempFile.deleteOnExit();

        final File testTemplate = new File(tempDir, "testtemplate.vm");
        testTemplate.deleteOnExit();

        final FileWriter out = new FileWriter(testTemplate);
        out.write("<p>Now is the winter of our $item</p>\n");
        out.close();

        unit.setTemplateDirectory(tempDir.getAbsolutePath());

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        final Map<String, Object> templateProperties = new HashMap<String, Object>();
        templateProperties.put("item", "discontent");

        unit.send(TO_EXAMPLE, FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "testtemplate", templateProperties);

        verify(mailSender).send(mimeMessage);
        verifyExpectationsForMimeMessage(FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "<p>Now is the winter of our discontent</p>\n", TO_EXAMPLE);
    }

    @Test(expected = EmailException.class)
    public void shouldThrowExceptionForInvalidFSPath() throws EmailException {
        unit.setTemplateDirectory("/nowhere/possibly/I/hope");

        unit.send(TO_EXAMPLE, FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "testtemplate", null);
    }

    @Test(expected = EmailException.class)
    public void shouldThrowExceptionForInvalidTemplate() throws EmailException {

        unit.send(TO_EXAMPLE, FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "demo-not-existing", null);
    }

    @Test(expected = EmailException.class)
    public void shouldThrowExceptionOnSendFailure() throws EmailException, MessagingException {

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("it's all gone bad")).when(mailSender).send(mimeMessage);

        final Map<String, Object> templateProperties = new HashMap<String, Object>();
        templateProperties.put("subject", "world");

        unit.send(TO_EXAMPLE, FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, "demo", templateProperties);
        verifyExpectationsForMimeMessage(FROM_ADDRESS_DEFAULT, SUBJECT_DEFAULT, CONTENT_DEFAULT, TO_EXAMPLE);
    }

    private void verifyExpectationsForMimeMessage(String fromAddress, String subject, String content, String... toAddresses) throws MessagingException {
        if (subject != null) {
            verify(mimeMessage).setSubject(subject);
        }

        InternetAddress[] expected = new InternetAddress[toAddresses.length];
        for (int i = 0; i < toAddresses.length; i++) {
            expected[i] = new InternetAddress(toAddresses[i]);
        }
        verify(mimeMessage).setRecipients(eq(Message.RecipientType.TO), aryEq(expected));
        verify(mimeMessage).setFrom(new InternetAddress(fromAddress));
        verify(mimeMessage).setContent(content, "text/html");
    }
}
