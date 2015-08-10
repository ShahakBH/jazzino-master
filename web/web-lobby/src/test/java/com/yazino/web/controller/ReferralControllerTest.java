package com.yazino.web.controller;

import com.yazino.web.util.CookieHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReferralControllerTest {
    @Mock
    private CookieHelper cookieHelper;

    @Test
    public void shouldGetReferralAndSetMatchingCookies() throws IOException {
        // GIVEN the controller using a cookie helper
        final ReferralController underTest = new ReferralController(cookieHelper);

        // WHEN sending the referrals to the controller
        final HttpServletResponse response = mock(HttpServletResponse.class);
        underTest.refer("ref", "url", "source", mock(HttpServletRequest.class), response);

        // THEN the passed parameters are set to cookies
        verify(cookieHelper).setReferralPlayerId(response, "ref");
        verify(cookieHelper).setScreenSource(response, "source");
    }

    @Test
    public void shouldDoNoActionWithoutMandatoryParameters() throws IOException {
        // GIVEN the controller using a cookie helper
        final ReferralController underTest = new ReferralController(cookieHelper);

        // WHEN sending the referrals to the controller without mandatory parameters
        final HttpServletResponse response = mock(HttpServletResponse.class);
        underTest.refer(null, "url", "source", mock(HttpServletRequest.class), response);

        // THEN there is no action on cookies
        verifyNoMoreInteractions(cookieHelper);
    }
}
