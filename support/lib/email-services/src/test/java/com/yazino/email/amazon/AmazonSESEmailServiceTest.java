package com.yazino.email.amazon;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.*;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailException;
import com.yazino.email.EmailService;
import org.apache.commons.lang3.ObjectUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AmazonSESEmailServiceTest {

    private static final String TO_EXAMPLE = "to@example.com";
    private static final String SUBJECT = "Test 1";
    private static final String FROM_ADDRESS = "test@example.com";
    private static final String TEMPLATE_CONTENT = "<b>Hello, world</b>\n";
    private static final String TEMPLATE_NAME = "demo";
    private static final String TEST_TEMPLATE_NAME = "testtemplate";

    @Mock
    private AmazonSimpleEmailServiceClient client;
    @Mock
    private AmazonSESClientFactory clientFactory;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private AmazonSESEmailService underTest;

    @Before
    public void setUp() {
        final AmazonSESConfiguration clientConfiguration = new AmazonSESConfiguration();
        final SendEmailResult result = new SendEmailResult().withMessageId("aMessageId");

        when(yazinoConfiguration.getBoolean(EmailService.PROPERTY_EMAIL_ENABLED, true)).thenReturn(true);
        when(clientFactory.clientFor("anAccessKey", "aSecretKey", clientConfiguration))
                .thenReturn(client);
        when(client.sendEmail(any(SendEmailRequest.class))).thenReturn(result);

        underTest = new AmazonSESEmailService("anAccessKey", "aSecretKey", clientFactory, clientConfiguration, yazinoConfiguration);
    }

    @Test
    public void aMessageShouldBeSentToTheSESClientWhenEmailEnabledIsTrue() throws EmailException {
        underTest.send(TO_EXAMPLE, FROM_ADDRESS, SUBJECT, TEMPLATE_NAME, templateProperties());

        verify(client).sendEmail(argThat(is(anEmail()
                .to(TO_EXAMPLE)
                .from(FROM_ADDRESS)
                .withSubject(SUBJECT)
                .withHtmlBody(TEMPLATE_CONTENT).asRequest())));
    }

    @Test
    public void aMessageShouldNotBeSentToTheSESClientWhenEmailEnabledIsFalse() throws EmailException {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getBoolean(EmailService.PROPERTY_EMAIL_ENABLED, true)).thenReturn(false);

        underTest.send(TO_EXAMPLE, FROM_ADDRESS, SUBJECT, TEMPLATE_NAME, templateProperties());

        verifyZeroInteractions(client);
    }

    @Test
    public void aMessageToMultipleRecipientsShouldBeSentToTheSESClient() throws EmailException {
        underTest.send(new String[]{"user1@example.com", "user2@example.com"}, FROM_ADDRESS,
                SUBJECT, TEMPLATE_NAME, templateProperties());

        verify(client).sendEmail(argThat(is(anEmail()
                .to("user1@example.com")
                .to("user2@example.com")
                .from(FROM_ADDRESS)
                .withSubject(SUBJECT)
                .withHtmlBody(TEMPLATE_CONTENT).asRequest())));
    }

    @Test
    public void aMessageWithoutASubjectShouldBeSentToTheSESClient() throws EmailException {
        underTest.send(TO_EXAMPLE, FROM_ADDRESS, null, TEMPLATE_NAME, templateProperties());

        verify(client).sendEmail(argThat(is(anEmail()
                .to(TO_EXAMPLE)
                .from(FROM_ADDRESS)
                .withHtmlBody(TEMPLATE_CONTENT).asRequest())));
    }

    @Test
    public void aMessageMissingTemplatePropertiesShouldBeSentToTheSESClient() throws EmailException {
        underTest.send(TO_EXAMPLE, FROM_ADDRESS, null, TEMPLATE_NAME, null);

        verify(client).sendEmail(argThat(is(anEmail()
                .to(TO_EXAMPLE)
                .from(FROM_ADDRESS)
                .withHtmlBody("<b>Hello, $subject</b>\n").asRequest())));
    }


    @Test
    public void whenATemplateDirectoryIsSpecifiedTemplatesWillBeSourcedFromIt() throws EmailException, IOException {
        final File templateDir = templateWith("<p>Now is the winter of our $item</p>\n");

        underTest.setTemplateDirectory(templateDir.getAbsolutePath());

        final Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("item", "discontent");

        underTest.send(TO_EXAMPLE, FROM_ADDRESS, SUBJECT, TEST_TEMPLATE_NAME, templateProperties);

        verify(client).sendEmail(argThat(is(anEmail()
                .to(TO_EXAMPLE)
                .from(FROM_ADDRESS)
                .withSubject(SUBJECT)
                .withHtmlBody("<p>Now is the winter of our discontent</p>\n").asRequest())));
    }

    @Test
    public void nullEmailAddressShouldThrowEmailException() {
        try {
            underTest.send((String) null, FROM_ADDRESS, SUBJECT, TEST_TEMPLATE_NAME, new HashMap<String, Object>());
            Assert.fail();
        } catch (EmailException e) {
            assertMissingEmailAddressException(e, "toAddress");
        }
    }

    @Test
    public void nullFromAddressShouldThrowEmailException() {
        try {
            underTest.send(TO_EXAMPLE, null, SUBJECT, TEST_TEMPLATE_NAME, new HashMap<String, Object>());
            Assert.fail();
        } catch (EmailException e) {
            assertMissingEmailAddressException(e, "fromAddress");
        }
    }

    @Test
    public void emptyEmailAddressShouldThrowEmailException() {
        try {
            underTest.send("", FROM_ADDRESS, SUBJECT, TEST_TEMPLATE_NAME, new HashMap<String, Object>());
            Assert.fail();
        } catch (EmailException e) {
            assertMissingEmailAddressException(e, "toAddress");
        }
    }

    @Test
    public void emptyFromAddressShouldThrowEmailException() {
        try {
            underTest.send(TO_EXAMPLE, "", SUBJECT, TEST_TEMPLATE_NAME, new HashMap<String, Object>());
            Assert.fail();
        } catch (EmailException e) {
            assertMissingEmailAddressException(e, "fromAddress");
        }
    }

    @Test
    public void aNullArrayOfEmailAddressShouldThrowAnException() {
        try {
            underTest.send((String[]) null, FROM_ADDRESS, SUBJECT, TEST_TEMPLATE_NAME, new HashMap<String, Object>());
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), is(equalTo("toAddresses may not be null/empty")));
        }
    }

    @Test
    public void anEmptyArrayOfEmailAddressShouldThrowAnException() {
        try {
            underTest.send(new String[0], FROM_ADDRESS, SUBJECT, TEST_TEMPLATE_NAME, new HashMap<String, Object>());
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), is(equalTo("toAddresses may not be null/empty")));
        }
    }

    @Test
    public void anArrayContainingAMalformedEmailAddressShouldThrowEmailException() {
        try {
            underTest.send(new String[]{"bob", "sam@somewhere", "fred@"}, FROM_ADDRESS,
                    SUBJECT, TEST_TEMPLATE_NAME, new HashMap<String, Object>());
            Assert.fail();
        } catch (EmailException e) {
            assertThat(e.getMessage(), is(anyOf(startsWith("Invalid email addresses submitted: fred@,bob"),
                    startsWith("Invalid email addresses submitted: bob,fred@"))));
        }
    }

    private void assertMissingEmailAddressException(final EmailException e, final String address) {
        assertThat(e.getMessage(), is(equalTo(address + " may not be null/empty")));
    }

    @Test(expected = EmailException.class)
    public void anInvalidTemplatePathWillCauseAnEmailException() throws EmailException {
        underTest.setTemplateDirectory("/nowhere/possibly/I/hope");

        underTest.send(TO_EXAMPLE, FROM_ADDRESS, SUBJECT, TEST_TEMPLATE_NAME, null);
    }

    @Test(expected = EmailException.class)
    public void anInvalidTemplateNameWillCauseAnEmailException() throws EmailException {
        underTest.send(TO_EXAMPLE, FROM_ADDRESS, SUBJECT, "demo-not-existing", null);
    }

    @Test(expected = EmailException.class)
    public void ifTheSendFailsThenAnEmailExceptionWillBeThrown() throws EmailException {
        reset(client);
        when(client.sendEmail(any(SendEmailRequest.class))).thenThrow(new AmazonServiceException("aTestException"));

        underTest.send(TO_EXAMPLE, FROM_ADDRESS, SUBJECT, TEMPLATE_NAME, templateProperties());
    }

    @Test
    public void ifTheAddressIsBlacklistedThenAnEmailExceptionWillNotBeThrown() throws EmailException {
        reset(client);
        when(client.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(new MessageRejectedException("Address blacklisted."));

        underTest.send(TO_EXAMPLE, FROM_ADDRESS, SUBJECT, TEMPLATE_NAME, templateProperties());
    }

    private EmailBuilder anEmail() {
        return new EmailBuilder();
    }

    private File templateWith(final String content) throws IOException {
        final File tempFile = File.createTempFile("dummy", "tmp");
        final File tempDir = tempFile.getParentFile();
        tempFile.deleteOnExit();

        final File testTemplate = new File(tempDir, TEST_TEMPLATE_NAME + ".vm");
        testTemplate.deleteOnExit();

        final FileWriter out = new FileWriter(testTemplate);
        out.write(content);
        out.close();
        return tempDir;
    }

    private class EmailBuilder {
        private final List<String> destinations = new ArrayList<>();
        private String body;
        private String from;
        private String subject;

        public EmailBuilder to(final String destination) {
            destinations.add(destination);
            return this;
        }

        public EmailBuilder from(final String from) {
            this.from = from;
            return this;
        }

        public EmailBuilder withSubject(final String subject) {
            this.subject = subject;
            return this;
        }

        public EmailBuilder withHtmlBody(final String body) {
            this.body = body;
            return this;
        }

        private SendEmailRequest buildRequest() {
            return new SendEmailRequest(from, new Destination(destinations),
                    new Message(new Content(subject),
                            new Body().withHtml(new Content(body).withCharset("UTF-8"))));
        }

        public Matcher<SendEmailRequest> asRequest() {
            return new TypeSafeMatcher<SendEmailRequest>() {
                @Override
                public boolean matchesSafely(final SendEmailRequest sendEmailRequest) {
                    boolean result = ObjectUtils.equals(sendEmailRequest.getDestination().getToAddresses(), destinations);
                    result &= sendEmailRequest.getDestination().getCcAddresses().isEmpty();
                    result &= sendEmailRequest.getDestination().getBccAddresses().isEmpty();
                    result &= ObjectUtils.equals(sendEmailRequest.getSource(), from);
                    result &= ObjectUtils.equals(sendEmailRequest.getMessage().getSubject().getData(), subject);
                    result &= ObjectUtils.equals(sendEmailRequest.getMessage().getBody().getHtml().getData(), body);
                    result &= ObjectUtils.equals(sendEmailRequest.getMessage().getBody().getHtml().getCharset(), "UTF-8");
                    return result;
                }

                @Override
                public void describeTo(final Description description) {
                    description.appendText("is equal to ").appendValue(buildRequest());
                }
            };
        }
    }

    private Map<String, Object> templateProperties() {
        final Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("subject", "world");
        return templateProperties;
    }
}
