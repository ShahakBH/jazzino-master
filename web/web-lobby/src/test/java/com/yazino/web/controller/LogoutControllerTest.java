package com.yazino.web.controller;

import com.yazino.web.security.LogoutHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LogoutControllerTest {
    @Mock
    private LogoutHelper logoutHelper;
    @Mock
    private HttpSession session;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private LogoutController underTest;

    @Before
    public void setUp() {
        underTest = new LogoutController(logoutHelper, "/logoutActionResult");
    }

    @Test
    public void theBlockedHandlerLogsThePlayerOut() {
        underTest.blocked(session, request, response, null);

        verify(logoutHelper).logout(session, request, response);
    }

    @Test
    public void theBlockedHandlerReturnsTheBlockedView() {
        final ModelAndView mav = underTest.blocked(session, request, response, null);

        assertThat(mav.getViewName(), is(equalTo("blocked")));
    }

    @Test
    public void theBlockedHandlerAddsNoReasonToTheModelByDefault() {
        final ModelAndView mav = underTest.blocked(session, request, response, null);

        assertThat(mav.getModel().get("reason"), is(nullValue()));
    }

    @Test
    public void theBlockedHandlerAddsTheReasonToTheModelIfPresent() {
        final ModelAndView mav = underTest.blocked(session, request, response, "payment");

        assertThat((String) mav.getModel().get("reason"),
                is(equalTo("Sorry, your card provider has not authorised this transaction. For your protection, we have automatically blocked your Yazino account.")));
    }

    @Test
    public void theBlockedHandlerIgnoresUnknownReasons() {
        final ModelAndView mav = underTest.blocked(session, request, response, "unknownReason");

        assertThat(mav.getModel().get("reason"), is(nullValue()));
    }

}
