package com.yazino.web.controller.profile;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultProfileTabRedirectorTest {

    private static final String ROOT_URI = "/player";
    private static final String DEFAULT_TAB = "profile";
    private static final String PARTIAL_PARAM_KEY = "partial";
    private static final String PARTIAL_PARAM_IN_QUERY_STRING = "?" + PARTIAL_PARAM_KEY + "=true";
    private static final String DEFAULT_TAB_URI = ROOT_URI + "/" + DEFAULT_TAB;
    private static final String DEFAULT_PARTIAL_TAB_URI = ROOT_URI + "/" + DEFAULT_TAB + PARTIAL_PARAM_IN_QUERY_STRING;

    private HttpServletRequest request;
    
    private DefaultProfileTabRedirector underTest;

    @Before
    public void setUp() {
        underTest = new DefaultProfileTabRedirector();
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void shouldRedirectToDefaultTab() throws UnsupportedEncodingException {
        RedirectView response = (RedirectView) underTest.defaultTabRedirect(request);

        assertEquals(DEFAULT_TAB_URI, response.getUrl());
    }

    @Test
    public void shouldRedirectWithPartialParam() throws UnsupportedEncodingException {
        final String[] paramValues = {"true"};
        when(request.getParameterValues(PARTIAL_PARAM_KEY)).thenReturn(paramValues);
        RedirectView response = (RedirectView) underTest.defaultTabRedirect(request);

        assertEquals(DEFAULT_PARTIAL_TAB_URI, response.getUrl());
    }

}
