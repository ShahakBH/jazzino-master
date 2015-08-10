package com.yazino.web.controller;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.Platform;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.tracking.TrackingEvent;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TrackingControllerTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(50);
    private static final String NAME = "some-event-name";
    private static final Platform PLATFORM = Platform.WEB;
    private static final DateTime CURRENT_TIME = new DateTime("2012-12-12T23:00:00");
    private static final String PROPERTIES = "{}";

    private LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private LobbySession lobbySession = mock(LobbySession.class);
    private QueuePublishingService queuePublishingService = mock(QueuePublishingService.class);
    private MockHttpServletRequest request = new MockHttpServletRequest();
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private TrackingController underTest = new TrackingController(lobbySessionCache, queuePublishingService);

    @Before
    public void setUp() throws Exception {
        given(lobbySessionCache.getActiveSession(Matchers.<HttpServletRequest>any())).willReturn(lobbySession);
        when(lobbySession.getPlatform()).thenReturn(PLATFORM);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
        request.setContent(PROPERTIES.getBytes());
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldReturnBadRequestWhenBodyIsNotWellFormed() throws IOException {
        request.setContent("\"boo\"".getBytes());

        underTest.trackEvent(request, response, NAME);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verifyZeroInteractions(queuePublishingService);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPublishTrackingEventWithSpecifiedValues() throws IOException {
        request.setContent("{ \"property1\": \"value1\", \"property2\": \"value2\" }".getBytes());

        underTest.trackEvent(request, response, NAME);

        ArgumentCaptor<TrackingEvent> event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(queuePublishingService).send(event.capture());
        assertEquals(PLATFORM, event.getValue().getPlatform());
        assertEquals(PLAYER_ID, event.getValue().getPlayerId());
        assertEquals(NAME, event.getValue().getName());
        Map<String, String> capturedProperties = event.getValue().getEventProperties();
        assertEquals("value1", capturedProperties.get("property1"));
        assertEquals("value2", capturedProperties.get("property2"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSendCurrentSystemTimeAsReceivedTime() throws IOException {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(CURRENT_TIME.getMillis());
        underTest.trackEvent(request, response, NAME);

        ArgumentCaptor<TrackingEvent> event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(queuePublishingService).send(event.capture());
        assertEquals(CURRENT_TIME, event.getValue().getReceived());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfSessionHasNullPlatform() {
        when(lobbySession.getPlatform()).thenReturn(null);
        underTest.trackEvent(request, response, NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfSessionHasNullPlayerId() {
        when(lobbySession.getPlayerId()).thenReturn(null);
        underTest.trackEvent(request, response, NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfNullName() {
        underTest.trackEvent(request, response, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfBlankName() {
        underTest.trackEvent(request, response, " ");
    }

    @Test
    public void shouldSendErrorWhenSpecifiedPropertiesNotValidMap() {
        request.setContent("\"value\"".getBytes());
        underTest.trackEvent(request, response, NAME);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldLogButNotThrowExceptions() throws IOException {
        doThrow(new RuntimeException("sample exception")).when(queuePublishingService).send(any(TrackingEvent.class));
        ListAppender appender = ListAppender.addTo(TrackingController.class);

        underTest.trackEvent(request, response, NAME);

        assertTrue(appender.getMessages().contains(
                String.format("Unable to track event (platform=%s, playerId=%s, name=%s)", PLATFORM, PLAYER_ID, NAME)));
    }
}
