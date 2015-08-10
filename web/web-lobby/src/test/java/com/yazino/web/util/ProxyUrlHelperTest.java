package com.yazino.web.util;

import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProxyUrlHelperTest {

    private static final String GOOGLE_CONTACTS_BASE_URL = "https://www.google.com/m8/feeds/contacts/default/full";//?access_token=ya29.AHES6ZTWmde5pKcVJCof4liAUpbkmPO6agm-g_x_EB9YKcssH2M70bE&alt=json";
    private static final String ACCESS_TOKEN = "sampleAccessToken";
    private static final String SAMPLE_PRIMARY_DOMAIN = "tubestop.london.yazino.com";
    private static final String SAMPLE_URI = "/google-api/oauth-callback#abc=def&alt=json";
    public static final String SAMPLE_PROXIED_URI = "/tubestop" + SAMPLE_URI;
    public static final String SAMPLE_PROXIED_HOST = "env-proxy.london.yazino.com";
    public static final boolean SECURE_REQUEST = true;
    public static final boolean INSECURE_REQUEST = false;
    private HttpClient httpClient = mock(HttpClient.class);
    private ProxyUrlHelper underTest;
    private HttpServletRequest request = mock(HttpServletRequest.class);

    @Before
    public void setUp() {
        underTest = new ProxyUrlHelper(SAMPLE_PRIMARY_DOMAIN);
        setupMockRequest(SAMPLE_PRIMARY_DOMAIN, SAMPLE_URI, SECURE_REQUEST);
    }

    @Test
    public void shouldDetectProxiedRequest() {
        setupMockRequest(SAMPLE_PROXIED_HOST, SAMPLE_PROXIED_URI, SECURE_REQUEST);

        assertTrue(underTest.isProxiedRequest(request));
    }

    @Test
    public void shouldDetectNonProxiedRequest() {
        setupMockRequest(SAMPLE_PRIMARY_DOMAIN, SAMPLE_URI, SECURE_REQUEST);

        assertFalse(underTest.isProxiedRequest(request));
    }

    @Test
    public void shouldRedirectToPrimaryDomainWhenProxied() {
        setupMockRequest(SAMPLE_PROXIED_HOST, SAMPLE_PROXIED_URI, SECURE_REQUEST);
        String result = underTest.createNonProxiedUrlFor(request);

        assertTrue("Assume that request is proxied", underTest.isProxiedRequest(request));
        assertThat(result, startsWith("https://" + SAMPLE_PRIMARY_DOMAIN));
    }

    @Test
    public void shouldRemoveDomainFromProxiedRequestAndPreserveProxiedRequestURI() {
        setupMockRequest(SAMPLE_PROXIED_HOST, SAMPLE_PROXIED_URI, SECURE_REQUEST);
        String result = underTest.createNonProxiedUrlFor(request);

        assertTrue("Assume that request is proxied", underTest.isProxiedRequest(request));
        assertThat(result, equalTo("https://" + SAMPLE_PRIMARY_DOMAIN + SAMPLE_URI));
    }

    @Test
    public void shouldIgnorePrimaryDomainElementsAppearingAfterFirstUriComponentWhenRedirecting() {
        final String uri = "/anything" + SAMPLE_PROXIED_URI;
        setupMockRequest(SAMPLE_PROXIED_HOST, uri, SECURE_REQUEST);
        String result = underTest.createNonProxiedUrlFor(request);

        assertTrue("Assume that request is proxied", underTest.isProxiedRequest(request));
        assertThat(result, equalTo("https://" + SAMPLE_PRIMARY_DOMAIN + uri));
    }

    @Test
    public void shouldPreserveProtocol() {
        setupMockRequest(SAMPLE_PROXIED_HOST, SAMPLE_PROXIED_URI, INSECURE_REQUEST);
        String result = underTest.createNonProxiedUrlFor(request);

        assertTrue("Assume that request is proxied", underTest.isProxiedRequest(request));
        assertThat(result, startsWith("http://"));

        setupMockRequest(SAMPLE_PROXIED_HOST, SAMPLE_PROXIED_URI, SECURE_REQUEST);
        result = underTest.createNonProxiedUrlFor(request);

        assertTrue("Assume that request is proxied", underTest.isProxiedRequest(request));
        assertThat(result, startsWith("https://"));
    }

    private void setupMockRequest(final String hostName, final String uri, final boolean isSecure) {
        when(request.getHeader("host")).thenReturn(hostName);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.isSecure()).thenReturn(isSecure == SECURE_REQUEST);
    }

}
