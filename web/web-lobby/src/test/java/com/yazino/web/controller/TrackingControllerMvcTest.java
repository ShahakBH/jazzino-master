package com.yazino.web.controller;

import com.yazino.platform.Platform;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.tracking.TrackingEvent;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TrackingControllerMvcTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(50);
    private static final Platform PLATFORM = Platform.WEB;

    private LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private LobbySession lobbySession = mock(LobbySession.class);
    private QueuePublishingService queuePublishingService = mock(QueuePublishingService.class);

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(new TrackingController(lobbySessionCache, queuePublishingService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        lobbySessionCache.setLobbySession(lobbySession);
        given(lobbySessionCache.getActiveSession(Matchers.<HttpServletRequest>any())).willReturn(lobbySession);
        when(lobbySession.getPlatform()).thenReturn(PLATFORM);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldReturnBadRequestWhenNoNameSpecified() throws Exception {
        mockMvc.perform(post("/tracking/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}".getBytes()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenSpecifiedPropertiesNotValidMap() throws Exception {
        mockMvc.perform(post("/tracking/event").param("name", "event1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"value\"".getBytes())).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWhenNoPropertiesSpecified() throws Exception {
        mockMvc.perform(post("/tracking/event").param("name", "event1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAllowWithEmptyProperties() throws Exception {
        mockMvc.perform(post("/tracking/event").param("name", "event1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}".getBytes()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldUseSpecifiedName() throws Exception {
        mockMvc.perform(post("/tracking/event").param("name", "event1").contentType(MediaType.APPLICATION_JSON)
                .content("{}".getBytes()));

        ArgumentCaptor<TrackingEvent> event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(queuePublishingService).send(event.capture());
        assertEquals("event1", event.getValue().getName());
    }

    @Test
    public void shouldUseSpecifiedProperties() throws Exception {
        mockMvc.perform(post("/tracking/event").param("name", "event1").contentType(MediaType.APPLICATION_JSON)
                .content(("{ \"p1\": \"value1\", \"p2\": \"value2\" }").getBytes()));

        ArgumentCaptor<TrackingEvent> event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(queuePublishingService).send(event.capture());
        assertEquals("value1", event.getValue().getEventProperties().get("p1"));
        assertEquals("value2", event.getValue().getEventProperties().get("p2"));
    }

    @Test
    public void shouldReturnNoContentWhenSuccessful() throws Exception {
        mockMvc.perform(post("/tracking/event")
                .param("name", "event1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}".getBytes()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturnUnauthorizedWhenNoSession() throws Exception {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(null);
        mockMvc.perform(post("/tracking/event")
                .param("name", "event1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}".getBytes()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldReturnBadRequest() throws Exception {
        String properties = "{\"action_inviteFriends_cta\":\"top bar\",\"action_inviteFriends_gameType\":\"SLOTS\",\"_n\":\"action_inviteFriends\",\"_k\":\"34fbc265d3d4a74249104900b000a39fc1fc59e0\",\"_p\":\"7238677\",\"_t\":1354790993}";
        mockMvc.perform(post("/tracking/event")
                .param("name", "event1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(properties.getBytes()));

        ArgumentCaptor<TrackingEvent> event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(queuePublishingService).send(event.capture());
    }

}

