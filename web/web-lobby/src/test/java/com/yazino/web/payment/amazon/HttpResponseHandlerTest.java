package com.yazino.web.payment.amazon;

import com.yazino.web.util.WebApiResponses;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static junit.framework.Assert.assertFalse;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpResponseHandlerTest {

    public static final String CONTENT = "xyz";
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private HttpServletResponse response;
    private HttpResponseHandler underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new HttpResponseHandler(webApiResponses);
    }

    @Test
    public void shouldWriteTheStatusToResponses() throws IOException {
        underTest.safeWriteOk(response, CONTENT);

        verify(webApiResponses, times(1)).writeOk(response, CONTENT);
    }

    @Test
    public void shouldHandleExceptionOnWrite() throws IOException {
        willThrow(new IOException()).given(webApiResponses).writeOk(response, CONTENT);

        assertFalse(underTest.safeWriteOk(response, CONTENT));
    }

    @Test
    public void shouldWriteNoContentOnToResponse() throws IOException {
        underTest.safeWriteEmptyResponse(response, HttpStatus.SC_OK);

        verify(webApiResponses, times(1)).writeNoContent(response, HttpStatus.SC_OK);
    }

    @Test
    public void shouldHandleExceptionOnWriteNoContent() throws IOException {
        willThrow(new IOException()).given(webApiResponses).writeNoContent(response, HttpStatus.SC_OK);

        assertFalse(underTest.safeWriteEmptyResponse(response, HttpStatus.SC_OK));
    }
}
