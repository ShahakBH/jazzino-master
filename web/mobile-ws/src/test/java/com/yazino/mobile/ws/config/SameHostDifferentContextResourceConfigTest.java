package com.yazino.mobile.ws.config;


import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SameHostDifferentContextResourceConfigTest {

    private final HttpServletRequest mRequest = mock(HttpServletRequest.class);

    @Test
    public void shouldSetBaseUrlAsEverythingUptoButExcludingContextPath() throws Exception {
        when(mRequest.getRequestURL()).thenReturn(new StringBuffer("http://foo.com/abc/d/e/f"));
        when(mRequest.getContextPath()).thenReturn("/abc");
        SameHostDifferentContextResourceConfig config = new SameHostDifferentContextResourceConfig(mRequest, "test");
        assertEquals("http://foo.com", config.getBaseUrl());
    }

    @Test
    public void shouldSetsDefaultContentUrlCorrectly() throws Exception {
        when(mRequest.getRequestURL()).thenReturn(new StringBuffer("http://foo.com/abc/d/e/f"));
        when(mRequest.getContextPath()).thenReturn("/abc");
        SameHostDifferentContextResourceConfig config = new SameHostDifferentContextResourceConfig(mRequest, "/test");
        assertEquals("http://foo.com/test", config.getContentUrl());
    }

    @Test
    public void shouldKeepPortNumbers() {
        when(mRequest.getRequestURL()).thenReturn(new StringBuffer("http://foo.com:999/abc/d/e/f"));
        when(mRequest.getContextPath()).thenReturn("/abc");
        SameHostDifferentContextResourceConfig config = new SameHostDifferentContextResourceConfig(mRequest, "test");
        assertEquals("http://foo.com:999", config.getBaseUrl());
    }

}
