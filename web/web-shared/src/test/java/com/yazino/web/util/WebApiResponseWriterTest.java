package com.yazino.web.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WebApiResponseWriterTest {
    @Mock
    private HttpServletResponse response = mock(HttpServletResponse.class);

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> data = Arrays.asList("A", "B", "C");

    private WebApiResponseWriter underTest = new WebApiResponseWriter();

    @Before
    public void setup() throws IOException {
        when(response.getWriter()).thenReturn(new PrintWriter(output));
    }

    @Test
    public void shouldSetContentTypeAsApplication_JSON() throws Exception {
        underTest.write(response, HttpStatus.SC_OK, data);

        verify(response).setContentType("application/json");
    }

    @Test
    public void shouldSetCharacterEncodingToUTF8() throws Exception {
        underTest.write(response, HttpStatus.SC_OK, data);

        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    public void shouldSetStatusAsSpecified() throws Exception {
        int expectedStatus = HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE;
        underTest.write(response, expectedStatus, data);

        verify(response).setStatus(expectedStatus);
    }

    @Test
    public void shouldWriteJSON() throws Exception {
        underTest.write(response, HttpStatus.SC_OK, data);

        assertThat(mapper.readValue(output.toString(), List.class), is(equalTo((List) data)));
    }

    @Test
    public void shouldFlushBuffer() throws Exception {
        underTest.write(response, HttpStatus.SC_OK, data);

        verify(response).flushBuffer();
    }

    @Test
    public void shouldNotAttemptToWriteIfTheResponseIsAlreadyCommitted() throws Exception {
        when(response.isCommitted()).thenReturn(true);
        doThrow(new IllegalStateException()).when(response).getWriter();

        underTest.write(response, HttpStatus.SC_OK, data);

        verify(response).isCommitted();
        verifyNoMoreInteractions(response);
    }

    @Test(expected = IOException.class)
    public void shouldThrowExceptionIfResponseIsBroken() throws Exception {
        doThrow(new SocketTimeoutException()).when(response).getWriter();

        underTest.write(response, HttpStatus.SC_OK, data);
    }
}
