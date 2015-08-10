package com.yazino.web.service;

import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.email.EmailBuilder;
import com.yazino.web.domain.email.EmailRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class QuietPlayerEmailerTest {

    private final PlayerProfileService profileService = mock(PlayerProfileService.class);
    private final AsyncEmailService emailService = mock(AsyncEmailService.class);
    private final EmailBuilder builder = mock(EmailBuilder.class);

    private String sender= "from@your.mum";
    private final QuietPlayerEmailer emailer = new QuietPlayerEmailer(emailService, profileService, sender);

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeConstructableWithNullEmailService() throws Exception {
        new QuietPlayerEmailer(null, profileService, sender);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeConstructableWithNullProfileService() throws Exception {
        new QuietPlayerEmailer(emailService, null, sender);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeConstructableWithNullDefaultFromAddress() throws Exception {
        new QuietPlayerEmailer(emailService, profileService, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenAttemptingToSendEmailWithNullBuilder() throws Exception {
        emailer.quietlySendEmail((EmailBuilder) null);
    }

    @Test(expected = NumberFormatException.class)
    public void shouldProprogateAnyExceptionThrownByTheBuilder() throws Exception {
        when(builder.buildRequest(profileService)).thenThrow(new NumberFormatException());
        emailer.quietlySendEmail(builder);
    }

    @Test
    public void shouldCatchAnyExceptionThrownByEmailServiceThatIsntEmailException() throws Exception {
        final EmailRequest request = mock(EmailRequest.class);
        when(builder.buildRequest(profileService)).thenReturn(request);
        doThrow(new NullPointerException()).when(emailService).send(
                anyCollectionOf(String.class), anyString(), anyString(), anyString(), anyMapOf(String.class, Object.class));
        assertFalse(emailer.quietlySendEmail(builder));
    }

    @Test
    public void shouldCallEmailServiceWithCorrectParameters() throws Exception {
        String template = "Template";
        String subject = "Subject";
        HashMap<String, Object> properties = new HashMap<String, Object>();
        Set<String> addresses = newHashSet("foo", "bar");
        EmailRequest request = new EmailRequest(template, subject, sender, properties,
                "foo", "bar");
        when(builder.buildRequest(profileService)).thenReturn(request);
        boolean sent = emailer.quietlySendEmail(builder);
        assertTrue(sent);
        verify(emailService).send(addresses,sender,  subject, "lobby/" + template, properties);
    }

    @Test
    public void shouldNotCallEmailServiceIfTheEmailAddressIsNull() throws Exception {
        EmailRequest request = new EmailRequest("aTemplate", "aSubject", sender, new HashMap<String, Object>());
        when(builder.buildRequest(profileService)).thenReturn(request);

        boolean sent = emailer.quietlySendEmail(builder);

        assertThat(sent, is(false));
        verifyZeroInteractions(emailService);
    }

    @Test
    public void shouldReturnFalseIfEmailServiceThrowsException() throws Exception {
        doThrow(new RuntimeException("Test")).when(emailService).send(anyCollectionOf(String.class), anyString(), anyString(), anyString(), anyMapOf(String.class, Object.class));
        EmailRequest request = mock(EmailRequest.class);
        when(builder.buildRequest(profileService)).thenReturn(request);
        boolean sent = emailer.quietlySendEmail(builder);
        assertFalse(sent);
    }

    @Test
    public void shouldReturnTrueIfEmailServiceSuccessfullySendsEmail() throws Exception {
        EmailRequest request = mock(EmailRequest.class);
        when(request.getAddresses()).thenReturn(asList("a@b.c"));
        when(builder.buildRequest(profileService)).thenReturn(request);
        boolean sent = emailer.quietlySendEmail(builder);
        assertTrue(sent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenAttemptingToSendEmailWithNullRequest_nonBuilder() throws Exception {
        emailer.quietlySendEmail((EmailRequest) null);
    }

    @Test
    public void shouldCatchAnyExceptionThrownByEmailService_nonBuilder() throws Exception {
        EmailRequest request = mock(EmailRequest.class);
        doThrow(new NullPointerException()).when(emailService).send(anySetOf(String.class), anyString(),anyString(), anyString(), anyMapOf(String.class, Object.class));

        assertFalse(emailer.quietlySendEmail(request));
    }

    @Test
    public void shouldCallEmailServiceWithCorrectParameters_nonBuilder() throws Exception {
        String template = "Template";
        String subject = "Subject";
        HashMap<String, Object> properties = new HashMap<String, Object>();
        Set<String> addresses = newHashSet("foo", "bar");
        String[] addressArray = addresses.toArray(new String[addresses.size()]);
        EmailRequest request = new EmailRequest(template, subject, "overriding@sender.com", properties, addressArray);

        boolean sent = emailer.quietlySendEmail(request);
        assertTrue(sent);
        verify(emailService).send(addresses, "overriding@sender.com", subject, QuietPlayerEmailer.TEMPLATE_GROUP + template, properties);
    }

    @Test
    public void builderWithNoSenderShouldUseDefaultSender(){
        String template = "Template";
        String subject = "Subject";
        HashMap<String, Object> properties = new HashMap<String, Object>();
        Set<String> addresses = newHashSet("foo", "bar");
        String[] addressArray = addresses.toArray(new String[addresses.size()]);
        EmailRequest request = new EmailRequest(template, subject, properties, addressArray);
        when(builder.buildRequest(profileService)).thenReturn(request);
        emailer.quietlySendEmail(builder);
        verify(emailService).send(addresses, sender, subject, QuietPlayerEmailer.TEMPLATE_GROUP + template, properties);
    }

    @Test
    public void shouldReturnFalseIfEmailServiceThrowsException_nonBuilder() throws Exception {
        doThrow(new RuntimeException("Test")).when(emailService).send(anySetOf(String.class), anyString(), anyString(), anyString(), anyMapOf(String.class, Object.class));
        EmailRequest request = mock(EmailRequest.class);
        boolean sent = emailer.quietlySendEmail(request);
        assertFalse(sent);
    }

    @Test
    public void shouldReturnTrueIfEmailServiceSuccessfullySendsEmail_nonBuilder() throws Exception {
        EmailRequest request = mock(EmailRequest.class);
        when(request.getAddresses()).thenReturn(asList("a@b.c"));
        boolean sent = emailer.quietlySendEmail(request);
        assertTrue(sent);
    }


}
