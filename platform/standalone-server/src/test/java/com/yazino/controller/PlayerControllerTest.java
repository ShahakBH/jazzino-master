package com.yazino.controller;

import com.yazino.host.session.StandaloneSessionService;
import com.yazino.model.StandalonePlayer;
import com.yazino.model.StandalonePlayerService;
import com.yazino.model.session.StandalonePlayerSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PlayerControllerTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private StandalonePlayerService playerService;
    private PlayerController underTest;
    private List<StandalonePlayer> players;
    private StandalonePlayerSession playerSession;
    private StandaloneSessionService standaloneSessionService;

    @Before
    public void setUp() {
        playerService = mock(StandalonePlayerService.class);
        playerSession = mock(StandalonePlayerSession.class);
        standaloneSessionService = mock(StandaloneSessionService.class);
        players = new ArrayList<StandalonePlayer>();
        when(playerService.findAll()).thenReturn(players);
        underTest = new PlayerController(playerService, playerSession, standaloneSessionService);
    }

    @Test
    public void shouldListAllAvailablePlayers() {
        final ModelAndView result = underTest.players();
        assertEquals(players, result.getModel().get("players"));
    }

    @Test
    public void shouldCreateNewPlayer() {
        final ModelAndView result = underTest.createPlayer("newPlayer", null);
        verify(playerService).createPlayer("newPlayer");
        verify(playerSession).setPlayer(any(BigDecimal.class), eq("newPlayer"));
        assertEquals("Player successfully created.", result.getModel().get("message"));
    }

    @Test
    public void shouldRedirectToReturnViewIfAvailable() {
        final ModelAndView result = underTest.createPlayer("newPlayer", "return");
        verify(playerService).createPlayer("newPlayer");
        verify(playerSession).setPlayer(any(BigDecimal.class), eq("newPlayer"));
        assertTrue(result.getView() instanceof RedirectView);
    }

}
