package com.yazino.web.interceptor;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class SecureRedirectionInterceptorTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ModelAndView modelAndView;

    private final Object handler = new Object();

    private SecureRedirectionInterceptor underTest;
    private ArrayList<String> allowedPaths;

    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);

        allowedPaths = Lists.newArrayList("/command/", "/publicCommand/", "/payment/");
        underTest = new SecureRedirectionInterceptor("https", allowedPaths);
    }

    @Test
    public void noActionIsTakenInPostHandle() throws Exception {
        underTest.postHandle(request, response, handler, modelAndView);

        verifyZeroInteractions(request, response, modelAndView);
    }

    @Test
    public void noActionIsTakenInAfterCompletion() throws Exception {
        underTest.afterCompletion(request, response, handler, null);

        verifyZeroInteractions(request, response);
    }

    @Test
    public void redirectionIsNotInvokedIfTheWebProtocolIsHttp() throws Exception {
        underTest = new SecureRedirectionInterceptor("http",allowedPaths);
        setRequestUrlTo("http://a.host:80/somewhere/else?a=valueofa");

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(true)));
        verifyZeroInteractions(request, response);
    }

    @Test
    public void redirectionIsInvokedIfTheRequestIsHttp() throws Exception {
        setRequestUrlTo("http://a.host:80/somewhere/else?a=valueofa");

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(false)));
        verify(response).sendRedirect("https://a.host:80/somewhere/else?a=valueofa");
    }

    @Test
    public void redirectionIsCorrectWhenNoQueryStringIsPresent() throws Exception {
        setRequestUrlTo("http://a.host:80/somewhere/else");

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(false)));
        verify(response).sendRedirect("https://a.host:80/somewhere/else");
    }

    @Test
    public void redirectionIsNotInvokedIfTheRequestProtocolIsHttps() throws Exception {
        setRequestUrlTo("https://a.host:80/somewhere/else?a=valueofa");

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(true)));
        verifyZeroInteractions(response);
    }

    @Test
    public void redirectionIsNotInvokedIfTheRequestedUrlIsACommand() throws Exception {
        setRequestUrlTo("http://a.host:80/command/something?a=valueofa");

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(true)));
        verifyZeroInteractions(response);
    }

    @Test
    public void redirectionIsNotInvokedIfTheRequestedUrlIsAPayment() throws Exception {
        setRequestUrlTo("http://a.host:80/payment/apaymentprovider/something?a=valueofa");

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(true)));
        verifyZeroInteractions(response);
    }

    @Test
    public void redirectionIsNotInvokedIfTheRequestedUrlIsAPublicCommand() throws Exception {
        setRequestUrlTo("http://a.host:80/publicCommand/something?a=valueofa");

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(true)));
        verifyZeroInteractions(response);
    }

    @Test
    public void redirectionIsNotInvokedForPostMethods() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.isSecure()).thenReturn(false);

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(true)));
        verifyZeroInteractions(response);
    }

    @Test
    public void redirectionIsNotInvokedIfTheRequestProtocolIsNotHttp() throws Exception {
        setRequestUrlTo("gopher://a.host:80/somewhere/else?a=valueofa");

        final boolean result = underTest.preHandle(request, response, handler);

        assertThat(result, is(equalTo(true)));
        verifyZeroInteractions(response);
    }

    private void setRequestUrlTo(final String url) throws URISyntaxException {
        reset(request);

        final URI uri = new URI(url);

        when(request.getMethod()).thenReturn("GET");
        when(request.isSecure()).thenReturn(uri.getScheme().equals("https"));
        when(request.getScheme()).thenReturn(uri.getScheme());
        when(request.getPathInfo()).thenReturn(uri.getPath());
        final String requestUrl = String.format("%s://%s:%s%s", uri.getScheme(),
                uri.getHost(), uri.getPort(), uri.getPath());
        when(request.getRequestURL()).thenReturn(new StringBuffer(requestUrl));
        when(request.getQueryString()).thenReturn(uri.getQuery());
    }

}
