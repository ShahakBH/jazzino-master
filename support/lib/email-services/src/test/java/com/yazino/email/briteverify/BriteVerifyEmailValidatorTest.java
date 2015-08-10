package com.yazino.email.briteverify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailVerificationResult;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.email.EmailVerificationStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BriteVerifyEmailValidatorTest {
    private static final String VALID_EMAIL = "aValidEmail@example.com";
    private static final String INVALID_EMAIL = "anInvalidEmail@example.com";
    private static final String UNKNOWN_EMAIL = "anUnknownEmail@example.com";
    private static final String ACCEPT_ALL_EMAIL = "anAcceptAllEmail@example.com";
    private static final String SERVER_FAIL_EMAIL = "aServerFailureEmail@example.com";
    private static final String DISPOSABLE_EMAIL = "aDisposableEmail@example.com";
    private static final String ROLE_EMAIL = "aRoleEmail@example.com";
    private static final String BRITE_VERIFY_URL = "http://brite.verify/url";
    private static final String API_KEY = "anApiKey";

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private HttpClient httpClient;

    private BriteVerifyEmailValidator underTest;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        underTest = new BriteVerifyEmailValidator(httpClient, yazinoConfiguration);

        when(yazinoConfiguration.getString("email.briteverify.url")).thenReturn(BRITE_VERIFY_URL);
        when(yazinoConfiguration.getString("email.briteverify.api-key")).thenReturn(API_KEY);

        mockResponseFor(VALID_EMAIL, aResponseWith(200, detailsFor(VALID_EMAIL, "valid")));
        mockResponseFor(UNKNOWN_EMAIL, aResponseWith(200, detailsFor(UNKNOWN_EMAIL, "unknown")));
        mockResponseFor(ACCEPT_ALL_EMAIL, aResponseWith(200, detailsFor(ACCEPT_ALL_EMAIL, "accept_all")));
        mockResponseFor(INVALID_EMAIL, aResponseWith(200, invalidDetailsFor(INVALID_EMAIL, "an_error_code")));
        mockResponseFor(DISPOSABLE_EMAIL, aResponseWith(200, addTo(detailsFor(DISPOSABLE_EMAIL, "valid"), "disposable", true)));
        mockResponseFor(ROLE_EMAIL, aResponseWith(200, addTo(detailsFor(DISPOSABLE_EMAIL, "valid"), "role_address", true)));
    }

    @Test
    public void aValidEmailReturnsAStatusOfValid() throws IOException, URISyntaxException {
        assertThat(underTest.validate(VALID_EMAIL), is(equalTo(new EmailVerificationResult(VALID_EMAIL, VALID))));
    }

    @Test
    public void aValidDisposableEmailReturnsTheDisposbleFlag() throws IOException, URISyntaxException {
        assertThat(underTest.validate(DISPOSABLE_EMAIL), is(equalTo(new EmailVerificationResult(DISPOSABLE_EMAIL, VALID, true, false))));
    }

    @Test
    public void aValidRoleEmailFailsReturnsTheRoleFlag() throws IOException, URISyntaxException {
        assertThat(underTest.validate(ROLE_EMAIL), is(equalTo(new EmailVerificationResult(ROLE_EMAIL, VALID, false, true))));
    }

    @Test
    public void anInvalidEmailReturnsAStatusOfInvalid() throws IOException, URISyntaxException {
        assertThat(underTest.validate(INVALID_EMAIL), is(equalTo(new EmailVerificationResult(INVALID_EMAIL, INVALID))));
    }

    @Test
    public void aMalformedEmailWithNoAtReturnsAStatusOfMalformed() throws IOException, URISyntaxException {
        assertThat(underTest.validate("bob"), is(equalTo(new EmailVerificationResult("bob", MALFORMED))));
        verifyZeroInteractions(httpClient);
    }

    @Test
    public void aMalformedEmailWithSpacesInTheUsernameReturnsAStatusOfMalformed() throws IOException, URISyntaxException {
        assertThat(underTest.validate("judith lynnharris345@hotmail.com"), is(equalTo(new EmailVerificationResult("judith lynnharris345@hotmail.com", MALFORMED))));
        verifyZeroInteractions(httpClient);
    }

    @Test
    public void aMalformedEmailWithNoServerPartReturnsAStatusOfMalformed() throws IOException, URISyntaxException {
        assertThat(underTest.validate("bob@"), is(equalTo(new EmailVerificationResult("bob@", MALFORMED))));
        verifyZeroInteractions(httpClient);
    }

    @Test
    public void anUnknownEmailReturnsAStatusOfUnknown() throws IOException, URISyntaxException {
        assertThat(underTest.validate(UNKNOWN_EMAIL), is(equalTo(new EmailVerificationResult(UNKNOWN_EMAIL, UNKNOWN))));
    }

    @Test
    public void anAcceptAllEmailReturnsAStatusOfAcceptAll() throws IOException, URISyntaxException {
        assertThat(underTest.validate(ACCEPT_ALL_EMAIL), is(equalTo(new EmailVerificationResult(ACCEPT_ALL_EMAIL, ACCEPT_ALL))));
    }

    @Test
    public void anEmailReturnsAStatusOfTempUnknownWhenTheBriteVerifyUrlIsMissing() {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getString("email.briteverify.api-key")).thenReturn(API_KEY);

        assertThat(underTest.validate(VALID_EMAIL), is(equalTo(new EmailVerificationResult(VALID_EMAIL, UNKNOWN_TEMPORARY))));
    }

    @Test
    public void anEmailReturnsAStatusOfTempUnknownWhenTheBriteVerifyUrlIsInvalid() {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getString("email.briteverify.url")).thenReturn("://aninvalidurl:80:800");
        when(yazinoConfiguration.getString("email.briteverify.api-key")).thenReturn(API_KEY);

        assertThat(underTest.validate(VALID_EMAIL), is(equalTo(new EmailVerificationResult(VALID_EMAIL, UNKNOWN_TEMPORARY))));
    }

    @Test
    public void anEmailReturnsAStatusOfTempUnknownWhenTheBriteVerifyApiKeyIsMissing() throws IOException, URISyntaxException {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getString("email.briteverify.url")).thenReturn(BRITE_VERIFY_URL);

        assertThat(underTest.validate(VALID_EMAIL), is(equalTo(new EmailVerificationResult(VALID_EMAIL, UNKNOWN_TEMPORARY))));
    }

    @Test
    public void anEmailReturnsAStatusOfTempUnknownWhenTheBriteVerifyServiceReturnsAFailureStatus() throws IOException, URISyntaxException {
        mockResponseFor(SERVER_FAIL_EMAIL, aResponseWith(500, detailsFor(SERVER_FAIL_EMAIL, "valid")));

        assertThat(underTest.validate(SERVER_FAIL_EMAIL), is(equalTo(new EmailVerificationResult(SERVER_FAIL_EMAIL, UNKNOWN_TEMPORARY))));
    }

    private void mockResponseFor(final String emailAddress, final HttpResponse response)
            throws IOException, URISyntaxException {
        when(httpClient.execute(argThat(hasGetUrl(urlForAddress(emailAddress))))).thenReturn(response);
    }

    private Map<String, Object> invalidDetailsFor(final String address,
                                                  final String errorCode) {
        final Map<String, Object> details = detailsFor(address, "invalid");
        details.put("error_code", errorCode);
        details.put("error", errorCode + "ButHumanReadable");
        return details;
    }

    private Map<String, Object> detailsFor(final String address,
                                           final String status) {
        final HashMap<String, Object> details = new HashMap<String, Object>();
        details.put("address", address);
        details.put("account", address.substring(0, address.indexOf("@")));
        details.put("domain", address.substring(address.indexOf("@") + 1));
        details.put("status", status);
        details.put("connected", true);
        details.put("disposable", false);
        details.put("role_address", false);
        details.put("duration", 0.104516605);
        return details;
    }

    private String urlForAddress(final String emailAddress) throws UnsupportedEncodingException {
        return BRITE_VERIFY_URL + "?address=" + URLEncoder.encode(emailAddress, "UTF-8")
                + "&apikey=" + URLEncoder.encode(API_KEY, "UTF-8");
    }

    private Matcher<HttpGet> hasGetUrl(final String url) throws URISyntaxException {
        final URI uri = new URI(url);
        return new TypeSafeMatcher<HttpGet>() {
            @Override
            protected boolean matchesSafely(final HttpGet item) {
                return item.getURI().equals(uri);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("does not have URI ").appendValue(url);
            }
        };
    }

    private Map<String, Object> addTo(final Map<String, Object> mad, final String key, final Object value) {
        mad.put(key, value);
        return mad;
    }

    private HttpResponse aResponseWith(final int statusCode,
                                       final Map<String, Object> content)
            throws IOException {
        final String jsonContent = new ObjectMapper().writeValueAsString(content);
        final ByteArrayInputStream contentStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));

        final HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(contentStream);

        final StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(statusCode);

        final HttpResponse response = mock(HttpResponse.class);
        when(response.getEntity()).thenReturn(entity);
        when(response.getStatusLine()).thenReturn(statusLine);

        return response;
    }

}
