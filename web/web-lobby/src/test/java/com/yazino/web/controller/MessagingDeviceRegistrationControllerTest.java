package com.yazino.web.controller;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.android.MessagingDeviceRegistrationEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.AMAZON;
import static com.yazino.platform.Platform.ANDROID;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class MessagingDeviceRegistrationControllerTest {

    private final static String DEVICE_TOKEN = "DEVICE_TOKEN";
    private final static BigDecimal PLAYER_ID = BigDecimal.valueOf(141);
    private final static String GAME_TYPE = "GAME_TYPE";
    private final static String REGISTRATION_ID = "REGISTRATION_ID";
    private final static String APP_ID = "TEST_APP_ID";
    private final static String DEVICE_ID = "TEST_DEVICE_ID";

    private LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private LobbySession lobbySession = mock(LobbySession.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private QueuePublishingService<MessagingDeviceRegistrationEvent> deviceRegistrationService = mock(QueuePublishingService.class);
    private final WebApiResponses responseWriter = mock(WebApiResponses.class);

    private MessagingDeviceRegistrationController underTest;

    @Before
    public void setUp() {
        underTest = new MessagingDeviceRegistrationController(lobbySessionCache, deviceRegistrationService, responseWriter);

        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
        when(lobbySession.getPlatform()).thenReturn(AMAZON);
    }

    @Test
    public void storeRegistrationIdShouldReturnA400IfThereIsNoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(null);
        underTest.storeRegistrationId(request, response, GAME_TYPE, REGISTRATION_ID);

        verify(responseWriter).writeError(response, HttpStatus.UNAUTHORIZED.value(), "no session");
    }

    @Test
    public void storeRegistrationIdShouldReturnInternalServerErrorIfRegistrationEventCannotBePutOnQueue() throws IOException {
        doThrow(new RuntimeException("sample exception")).when(deviceRegistrationService).send(any(MessagingDeviceRegistrationEvent.class));

        underTest.storeRegistrationId(request, response, GAME_TYPE, REGISTRATION_ID);

        verify(responseWriter).writeError(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), "could not add registration message to queue, please try again later.");
    }

    @Test
    public void storeDeviceAndRegistrationIdShouldPutMessageDeviceRegistrationEventOnQueue() throws IOException {
        underTest.storeDeviceAndRegistrationId(request, response, GAME_TYPE, APP_ID, DEVICE_ID, REGISTRATION_ID);

        MessagingDeviceRegistrationEvent expectedMessage = new MessagingDeviceRegistrationEvent(PLAYER_ID, GAME_TYPE, REGISTRATION_ID, AMAZON);
        expectedMessage.setAppId(APP_ID);
        expectedMessage.setDeviceId(DEVICE_ID);
        verify(deviceRegistrationService).send(expectedMessage);
    }

    @Test
    public void storeRegistrationIdShouldPutMessageDeviceRegistrationEventOnQueue() throws IOException {
        underTest.storeRegistrationId(request, response, GAME_TYPE, REGISTRATION_ID);

        MessagingDeviceRegistrationEvent expectedMessage = new MessagingDeviceRegistrationEvent(PLAYER_ID, GAME_TYPE, REGISTRATION_ID, AMAZON);
        verify(deviceRegistrationService).send(expectedMessage);
    }

    @Test
    public void shouldPublishDeviceRegistrationEvent() throws IOException {
        underTest.storeGCMRegistrationId(PLAYER_ID, GAME_TYPE, REGISTRATION_ID, request, response);

        MessagingDeviceRegistrationEvent expectedMessage =
                new MessagingDeviceRegistrationEvent(PLAYER_ID, GAME_TYPE, REGISTRATION_ID, ANDROID);
        verify(deviceRegistrationService).send(expectedMessage);
    }

    @Test
    public void shouldLogButNotThroughExceptions() throws IOException { // TODO review
        doThrow(new RuntimeException("sample exception")).when(deviceRegistrationService).send(any(MessagingDeviceRegistrationEvent.class));
        ListAppender appender = ListAppender.addTo(MessagingDeviceRegistrationController.class);

        underTest.storeGCMRegistrationId(PLAYER_ID, GAME_TYPE, REGISTRATION_ID, request, response);

        assertTrue(appender.getMessages().contains(
                String.format("Unable to update register device (playerId=%s, gameType=%s, registrationId=%s, platform=%s)",
                        PLAYER_ID, GAME_TYPE, REGISTRATION_ID, ANDROID)));
    }

    @Test
    public void shouldReturnForbiddenWhenNoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(null);

        underTest.storeGCMRegistrationId(PLAYER_ID, GAME_TYPE, REGISTRATION_ID, request, response);

        verify(response).sendError(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void shouldReturnForbiddenWhenPlayerIdDiffersFromLoggedInUser() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID.add(BigDecimal.ONE));

        underTest.storeGCMRegistrationId(PLAYER_ID, GAME_TYPE, REGISTRATION_ID, request, response);

        verify(response).sendError(HttpStatus.FORBIDDEN.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenNoPlayerId() throws IOException {
        underTest.storeGCMRegistrationId(null, GAME_TYPE, REGISTRATION_ID, request, response);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenNoGameType() throws IOException {
        underTest.storeGCMRegistrationId(PLAYER_ID, null, REGISTRATION_ID, request, response);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenNoRegistrationId() throws IOException {
        underTest.storeGCMRegistrationId(PLAYER_ID, GAME_TYPE, null, request, response);
    }

}
