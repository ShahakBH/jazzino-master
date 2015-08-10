package com.yazino.web.controller;

import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class VersionControllerTest {
    private static final String VERSION = "1.2.3-SNAPSHOT";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ServletContext servletContext;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private HttpSession session;

    private VersionController underTest;

    @Before
    public void setUp() throws UnsupportedEncodingException {
        MockitoAnnotations.initMocks(this);

        when(request.getSession()).thenReturn(session);
        when(session.getServletContext()).thenReturn(servletContext);

        when(servletContext.getResourceAsStream("/META-INF/maven/com.yazino/web-lobby/pom.properties"))
                .thenReturn(new ByteArrayInputStream(String.format("version=%s\n", VERSION).getBytes("UTF8")));

        underTest = new VersionController(webApiResponses);
    }

    @Test
    public void anInternalServerErrorShouldBeSentIfTheServletContextDoesNotExist() throws IOException {
        reset(session);

        underTest.showVersionInformation(request, response);

        verify(response).sendError(500);
    }

    @Test
    public void anInternalServerErrorShouldBeSentIfThePropertiesReadFails() throws IOException {
        reset(servletContext);
        when(servletContext.getResourceAsStream(anyString())).thenThrow(new RuntimeException("aTestException"));

        underTest.showVersionInformation(request, response);

        verify(response).sendError(500);
    }

    @Test
    public void theVersionShouldBeAddedToTheModelIfPomPropertiesIsPresent() throws IOException {
        underTest.showVersionInformation(request, response);

        assertThat((String) responseJson().get("version"), is(equalTo(VERSION)));
    }

    @Test
    public void theDevelopmentVersionShouldBeAddedToTheModelIfNoPomPropertiesIsPresent() throws IOException {
        reset(servletContext);

        underTest.showVersionInformation(request, response);

        assertThat((String) responseJson().get("version"), is(equalTo("development")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> responseJson() throws IOException {
        final ArgumentCaptor<Map> jsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), jsonCaptor.capture());
        return jsonCaptor.getValue();
    }

}
