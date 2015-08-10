package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import strata.server.lobby.api.promotion.InGameMessage;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class BuyChipsControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    private LobbySessionCache lobbySessionCache;

    @Mock
    private BuyChipsPromotionService buyChipsPromotionService;

    @Mock
    private HttpServletRequest request;

    private MockHttpServletResponse response;

    private BuyChipsController underTest;

    @Before
    public void init() {
        response = new MockHttpServletResponse();
        underTest = new BuyChipsController(buyChipsPromotionService, lobbySessionCache);
    }

    @Test
    public void responseHasCorrectContentType() throws IOException {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, false, WEB, AuthProvider.YAZINO);
        given(lobbySessionCache.getActiveSession(request)).willReturn(session);
        given(buyChipsPromotionService.getInGameMessageFor(PLAYER_ID)).willReturn(null);

        underTest.provideInGameMessage(request, response);

        assertThat(response.getContentType(), is("application/json"));
    }

    @Test
    public void whenNoActiveSessionShouldNotReturnGameMessage() throws IOException {
        underTest.provideInGameMessage(request, response);
        assertThat(response.getContentAsString(), is("{}"));
    }


    @Test
    public void whenPlayerIsInAPromotionShouldReturnGameMessage() throws IOException {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, false, WEB, AuthProvider.YAZINO);
        given(lobbySessionCache.getActiveSession(request)).willReturn(session);
        InGameMessage inGameMessage = new InGameMessage("header", "message");
        given(buyChipsPromotionService.getInGameMessageFor(PLAYER_ID, WEB)).willReturn(inGameMessage);

        underTest.provideInGameMessage(request, response);

        String expectedContent = new JsonHelper().serialize(inGameMessage);
        assertThat(response.getContentAsString(), is(expectedContent));
    }

    @Test
    public void whenPlayerIsNotInAPromotionShouldNotReturnGameMessage() throws IOException {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, false, WEB, AuthProvider.YAZINO);
        given(lobbySessionCache.getActiveSession(request)).willReturn(session);
        given(buyChipsPromotionService.getInGameMessageFor(PLAYER_ID, WEB)).willReturn(null);

        underTest.provideInGameMessage(request, response);

        assertThat(response.getContentAsString(), is("{}"));
    }
}
