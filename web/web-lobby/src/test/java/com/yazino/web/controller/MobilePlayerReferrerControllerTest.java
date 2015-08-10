package com.yazino.web.controller;

import com.yazino.platform.Platform;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MobilePlayerReferrerControllerTest {

    @Mock
    private QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService;

    private MobilePlayerReferrerController underTest;

    private static final int OK = 200;

    private static final String GAME_TYPE = "gameType";

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);
        underTest = new MobilePlayerReferrerController(playerReferrerEventService);
    }

    @Test
    public void shouldSendFailureResponseIfInvalidPlatform() throws Exception {

        final HttpServletResponse response1 = mock(HttpServletResponse.class);
        underTest.referrer("NOT_A_PLATFORM", GAME_TYPE, "1", "my_ref", response1);
        verify(response1).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldSendFailureResponseIfNoPlayerId() throws Exception {

        final HttpServletResponse response1 = mock(HttpServletResponse.class);
        underTest.referrer(Platform.IOS.name(), GAME_TYPE, null, "my_ref", response1);
        verify(response1).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldSendOKResponseIfValidRequest() throws Exception{
        final HttpServletResponse response1 = mock(HttpServletResponse.class);
        underTest.referrer(Platform.IOS.name(), GAME_TYPE, "1", "my_ref", response1);
        verify(response1).setStatus(OK);
    }

}
