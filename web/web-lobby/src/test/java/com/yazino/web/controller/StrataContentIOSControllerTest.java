package com.yazino.web.controller;

import org.junit.Test;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StrataContentIOSControllerTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    public void shouldReturnRedirectViewWithCorrectURLForImages() throws Exception {
        StringBuffer url = new StringBuffer("https://www.yazino.com/strata.content/mobile/ios/images/SLOTS/chips_package_4.png");
        when(request.getRequestURL()).thenReturn(url);
        String contentURL = "http://localhost:8080/web-content";
        StrataContentIOSController contoller = new StrataContentIOSController(contentURL);

        RedirectView view = contoller.redirect(request);
        assertEquals("http://localhost:8080/web-content/mobile/ios/images/SLOTS/chips_package_4.png", view.getUrl());
    }

    @Test
    public void shouldReturnRedirectViewWithCorrectURLForOtherContent() throws Exception {
        StringBuffer url = new StringBuffer("https://www.yazino.com/strata.content/mobile/ios/BLACKJACK/notifications.json");
        when(request.getRequestURL()).thenReturn(url);
        String contentURL = "http://cdn.yazino.com/web-content-12345";
        StrataContentIOSController contoller = new StrataContentIOSController(contentURL);

        RedirectView view = contoller.redirect(request);
        assertEquals("http://cdn.yazino.com/web-content-12345/mobile/ios/BLACKJACK/notifications.json", view.getUrl());
    }


}
