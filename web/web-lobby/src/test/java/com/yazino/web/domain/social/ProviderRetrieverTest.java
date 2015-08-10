package com.yazino.web.domain.social;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProviderRetrieverTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private PlayerProfileService service;
    private ProviderRetriever underTest;

    @Before
    public void setUp() throws Exception {
        service = mock(PlayerProfileService.class);
        underTest = new ProviderRetriever(service);
    }

    @Test
    public void shouldRetrieveName() {
        final PlayerProfile profile = PlayerProfile.withPlayerId(PLAYER_ID).withProviderName("FACEBOOK").asProfile();
        when(service.findByPlayerId(PLAYER_ID)).thenReturn(profile);
        assertEquals("FACEBOOK", underTest.retrieveInformation(PLAYER_ID, "aGameType"));
    }

    @Test
    public void shouldReturnNullIfProfileNotFound() {
        assertNull(underTest.retrieveInformation(PLAYER_ID, "aGameType"));
    }
}
