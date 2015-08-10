package com.yazino.web.interceptor;

import com.yazino.web.session.ReferrerSessionCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RefererInterceptorTest {

    @Mock
    private ReferrerSessionCache referrerSessionCache;
    @Mock
    private HttpServletRequest request;

    private RefererInterceptor underTest;

    @Before
    public void setUp() {
        when(request.getRequestURI()).thenReturn("/a/request");

        underTest = new RefererInterceptor(referrerSessionCache, asList("/blacklisted/request"));
    }

    @Test
    public void shouldDelegateReferrerResolutionToCache() throws Exception {
        underTest.preHandle(request, null, null);

        verify(referrerSessionCache).resolveReferrerFrom(request);
    }

    @Test
    public void shouldIgnoreRequestsToBlacklistedUrls() throws Exception {
        // This controller uses getParameter, which breaks request.getReader() for controllers
        // where the MIME type is application/x-www-form-urlencoded.
        // Unfortunately, the Flash client sends requests with this type. Hurrah. Hence we
        // need the ability to blacklist them.

        when(request.getRequestURI()).thenReturn("/blacklisted/request");

        underTest.preHandle(request, null, null);

        verify(referrerSessionCache, never()).resolveReferrerFrom(request);
    }

    @Test
    public void shouldIgnoreRequestsToChildrenOfBlacklistedUrls() throws Exception {
        // This controller uses getParameter, which breaks request.getReader() for controllers
        // where the MIME type is application/x-www-form-urlencoded.
        // Unfortunately, the Flash client sends requests with this type. Hurrah. Hence we
        // need the ability to blacklist them.

        when(request.getRequestURI()).thenReturn("/blacklisted/request/child");

        underTest.preHandle(request, null, null);

        verify(referrerSessionCache, never()).resolveReferrerFrom(request);
    }

}
