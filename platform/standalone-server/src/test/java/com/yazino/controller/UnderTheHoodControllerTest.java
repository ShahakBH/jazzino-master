package com.yazino.controller;

import com.yazino.model.ReadableGameStatusSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnderTheHoodControllerTest {

    private UnderTheHoodController underTest;
    private ReadableGameStatusSource gameStatusSource;

    @Before
    public void setUp() throws Exception {
        gameStatusSource = mock(ReadableGameStatusSource.class);
        underTest = new UnderTheHoodController(gameStatusSource, null, null, null);
    }

    @Test
    public void shouldGetReadableGameStatus() {
        when(gameStatusSource.getStatus()).thenReturn("some status");
        final ModelAndView modelAndView = underTest.gameStatus();
        assertEquals("some status", modelAndView.getModel().get("status"));
    }

}
