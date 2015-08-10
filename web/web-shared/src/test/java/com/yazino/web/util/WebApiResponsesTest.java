package com.yazino.web.util;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class WebApiResponsesTest {

    private HttpServletResponse response = mock(HttpServletResponse.class);
    private WebApiResponseWriter responseWriter = mock(WebApiResponseWriter.class);
    private List<String> data = Arrays.asList("A", "B", "C");
    private WebApiResponses underTest = new WebApiResponses(responseWriter);

    @Test
    public void writeShouldDelegateToResponseWriter() throws Exception {
        underTest.write(response, HttpStatus.SC_ACCEPTED, data);

        verify(responseWriter).write(same(response), eq(HttpStatus.SC_ACCEPTED), same(data));
    }

    @Test
    public void writeShouldDelegateToResponseWriterWithOK() throws Exception {
        underTest.writeOk(response, data);

        verify(responseWriter).write(same(response), eq(HttpStatus.SC_OK), same(data));
    }

    @Test
    public void writeShouldDelegateToResponseWriterWithNoContent() throws Exception {
        underTest.writeNoContent(response, HttpStatus.SC_FORBIDDEN);

        verify(responseWriter).write(same(response), eq(HttpStatus.SC_FORBIDDEN), eq(WebApiResponses.NO_CONTENT));
    }

    @Test
    public void writeShouldDelegateToResponseWriterWithGivenStatusAndErrorMessage() throws Exception {
        String expectedMessage = "arg is missing";
        underTest.writeError(response, HttpStatus.SC_BAD_REQUEST, expectedMessage);

        verify(responseWriter).write(same(response), eq(HttpStatus.SC_BAD_REQUEST), eq(new WebApiResponses.ErrorResponse(expectedMessage)));
    }
}
