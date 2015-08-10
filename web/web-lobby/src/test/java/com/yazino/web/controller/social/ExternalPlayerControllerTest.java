package com.yazino.web.controller.social;

import com.yazino.platform.player.service.PlayerProfileService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExternalPlayerControllerTest {

    private static final String PROVIDER = "myProvider";
    @Mock
    private PlayerProfileService playerProfileService;
    private ExternalPlayerController underTest;

    @Before
    public void setUp() {
        underTest = new ExternalPlayerController(playerProfileService);
    }

    @Test
    public void shouldRetrieveRegisteredPlayers_AnyProvider() {
        final HashSet<String> expected = new HashSet<String>(asList("1", "3"));
        when(playerProfileService.findRegisteredExternalIds(PROVIDER, "1", "2", "3")).thenReturn(expected);
        final Set<String> registeredPlayers = underTest.checkRegisteredFacebookUsers(PROVIDER, "1,2,3");
        assertEquals(expected, registeredPlayers);
    }

    @Test
    public void shouldRetrieveRegisteredPlayers_CandidatesWithSpace() {
        final HashSet<String> expected = new HashSet<String>(asList("21"));
        when(playerProfileService.findRegisteredExternalIds(PROVIDER, "1", "21", "3")).thenReturn(expected);
        final Set<String> registeredPlayers = underTest.checkRegisteredFacebookUsers(PROVIDER, "  1,21, 3");
        assertEquals(expected, registeredPlayers);
    }

    @Test
    public void shouldRetrieveRegisteredPlayers_EmptyCandidates() {
        final Set<String> registeredPlayers = underTest.checkRegisteredFacebookUsers(PROVIDER, "  ");
        assertEquals(0, registeredPlayers.size());
        verifyZeroInteractions(playerProfileService);
    }
}
