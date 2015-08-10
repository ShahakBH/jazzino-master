package com.yazino.web.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class RequestParameterUtilsTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void aNullParameterCausesAnInvalidRequestResponseCodeToBeSent() throws IOException {
        hasParameter("aParameter", null, request, response);

        verify(response).sendError(400);
    }

    @Test
    public void aNullParameterReturnsFalse() {
        assertThat(hasParameter("aParameter", null, request, response), is(false));
    }

    @Test
    public void aBlankStringParameterCausesAnInvalidRequestResponseCodeToBeSent() throws IOException {
        hasParameter("aParameter", "  ", request, response);

        verify(response).sendError(400);
    }

    @Test
    public void aBlankStringParameterReturnsFalse() {
        assertThat(hasParameter("aParameter", " ", request, response), is(false));
    }

    @Test
    public void aNonBlankParameterDoesNotModifyTheHttpResponse() {
        hasParameter("aParameter", "aValue", request, response);

        verifyZeroInteractions(response);
    }
    
    @Test
    public void aNonBlankParameterReturnsTrue() {
        assertThat(hasParameter("aParameter", "aValue", request, response), is(true));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void aNullParameterNameCausesANullPointerException() {
        hasParameter(null, "aValue", request, response);
    }
    
    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void aNullRequestCausesANullPointerException() {
        hasParameter("aParameter", "aValue", null, response);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void aNullResponseCausesANullPointerException() {
        hasParameter("aParameter", "aValue", request, null);
    }

}
