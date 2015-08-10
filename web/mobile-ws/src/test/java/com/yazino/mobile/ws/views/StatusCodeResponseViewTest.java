package com.yazino.mobile.ws.views;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatusCodeResponseViewTest {

    private final HttpServletRequest mRequest = mock(HttpServletRequest.class);
    private final HttpServletResponse mResponse = mock(HttpServletResponse.class);
    private final StatusCodeResponseView mView = new StatusCodeResponseView(10890);

    @Before
    public void setup() throws Exception {
        when(mRequest.getPathInfo()).thenReturn("http://foo.com/abc");
        PrintWriter writer = mock(PrintWriter.class);
        when(mResponse.getWriter()).thenReturn(writer);
    }
    
    @Test
    public void shouldWriteCorrectStatusCodeToResponse() throws Exception {
        mView.render(Collections.<String, Object>emptyMap(), mRequest, mResponse);
        verify(mResponse).setStatus(10890);
    }

}
