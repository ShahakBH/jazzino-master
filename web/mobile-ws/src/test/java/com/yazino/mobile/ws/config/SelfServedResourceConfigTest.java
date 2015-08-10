package com.yazino.mobile.ws.config;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SelfServedResourceConfigTest {

    private final HttpServletRequest mRequest = mock(HttpServletRequest.class);

    @Test
    public void shouldSetBaseUrlAsEverythingUptoAndIncludingContextPath() throws Exception {
        when(mRequest.getRequestURL()).thenReturn(new StringBuffer("http://foo.com/abc/d/e/f"));
        when(mRequest.getContextPath()).thenReturn("/abc");
        SelfServedResourceConfig config = new SelfServedResourceConfig(mRequest, "test");
        assertEquals("http://foo.com/abc", config.getBaseUrl());
    }
    
    @Test
    public void shouldSetsDefaultContentUrlCorrectly() throws Exception {
        when(mRequest.getRequestURL()).thenReturn(new StringBuffer("http://foo.com/abc/d/e/f"));
        when(mRequest.getContextPath()).thenReturn("/abc");
        SelfServedResourceConfig config = new SelfServedResourceConfig(mRequest, "/test");
        assertEquals("http://foo.com/abc/static/test", config.getContentUrl());
    }

    @Test
    public void shouldKeepPortNumbers() {
        when(mRequest.getRequestURL()).thenReturn(new StringBuffer("http://foo.com:999/abc/d/e/f"));
        when(mRequest.getContextPath()).thenReturn("/abc");
        SelfServedResourceConfig config = new SelfServedResourceConfig(mRequest, "test");
        assertEquals("http://foo.com:999/abc", config.getBaseUrl());
    }



}
