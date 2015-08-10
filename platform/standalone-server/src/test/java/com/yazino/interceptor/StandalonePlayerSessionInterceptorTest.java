package com.yazino.interceptor;

import com.yazino.model.session.StandalonePlayerSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StandalonePlayerSessionInterceptorTest {

    private StandalonePlayerSession session;
    private StandalonePlayerSessionInterceptor underTest;
    private ModelAndView modelAndView;

    @Before
    public void setUp() {
        modelAndView = mock(ModelAndView.class);
        session = mock(StandalonePlayerSession.class);
        underTest = new StandalonePlayerSessionInterceptor(session);
    }

    @Test
    public void shouldAddPlayerToModel() throws Exception {
        underTest.postHandle(null, null, null, modelAndView);
        verify(modelAndView).addObject("standalonePlayerSession", session);
    }

    @Test
    public void shouldCreateModelIfDoesNotExist() throws Exception {
        underTest.postHandle(null, null, null, null);
    }
}
