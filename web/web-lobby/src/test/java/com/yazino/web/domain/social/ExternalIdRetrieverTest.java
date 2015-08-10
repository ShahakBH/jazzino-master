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

public class ExternalIdRetrieverTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private PlayerProfileService service;
    private ExternalIdRetriever underTest;

    @Before
    public void setUp() throws Exception {
        service = mock(PlayerProfileService.class);
        underTest = new ExternalIdRetriever(service);
    }

    @Test
    public void shouldRetrieveName() {
        final PlayerProfile profile = PlayerProfile.withPlayerId(PLAYER_ID).withExternalId("myExternalId").asProfile();
        when(service.findByPlayerId(PLAYER_ID)).thenReturn(profile);
        assertEquals("myExternalId", underTest.retrieveInformation(PLAYER_ID, "aGameType"));
    }

    @Test
    public void shouldReturnNullIfProfileNotFound() {
        assertNull(underTest.retrieveInformation(PLAYER_ID, "aGameType"));
    }
}
