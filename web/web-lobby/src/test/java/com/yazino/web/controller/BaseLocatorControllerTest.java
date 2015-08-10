package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.session.LobbySession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ModelMap;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class BaseLocatorControllerTest {
    private static final Partner PARTNER_ID = Partner.YAZINO;
    private static final String GAME_TYPE = "BLACKJACK";
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    public static final String PLAYER_NAME = "playerName";
    public static final long COUNTDOWN_VALUE = Long.MAX_VALUE;

    @Mock
    private GameAvailabilityService gameAvailabilityService;

    private BaseLocatorController underTest;

    @Before
    public void setUpProperties() {
        MockitoAnnotations.initMocks(this);

        underTest = new UnderTestBaseLocatorControllerTest(gameAvailabilityService);

        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(new GameAvailability(GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED, COUNTDOWN_VALUE));
    }

    @Test
    public void shouldPopulateCommonLauncherPropertiesWithCorrectData() {
        assertModelMapValue("gameType", GAME_TYPE);
    }

    @Test
    public void shouldPopulatePLayerId() {
        assertModelMapValue("playerId", PLAYER_ID);
    }

    @Test
    public void shouldPopulatePLayerName() {
        assertModelMapValue("playerName", PLAYER_NAME);
    }

    @Test
    public void shouldHaveNoFacebookConfig() {
        assertModelMapValue("facebookApiKey", null);
        assertModelMapValue("facebookApplicationId", null);
    }

    @Test
    public void shouldPopulateCountdown() {
        assertModelMapValue("countdown", COUNTDOWN_VALUE);
    }

    @Test
    public void shouldNotPopulateCountdown() {
        reset(gameAvailabilityService);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(new GameAvailability(GameAvailabilityService.Availability.AVAILABLE));
        assertModelMapValue("countdown", null);
    }

    private void assertModelMapValue(String key, Object expectedValue, Partner partnerId) {
        ModelMap modelMap = new ModelMap();
        underTest.populateCommonLauncherProperties(modelMap, getLobbySession(partnerId), GAME_TYPE);
        assertEquals(String.format("Looking at model value for key [%s]", key), expectedValue, modelMap.get(key));
    }

    private void assertModelMapValue(String key, Object expectedValue) {
        assertModelMapValue(key, expectedValue, PARTNER_ID);
    }

    private LobbySession getLobbySession(Partner partnerId) {
        return new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, PLAYER_NAME, "senetSessionKey", partnerId, "pictureUrl", "email@email",
                null, false,
                Platform.WEB, AuthProvider.YAZINO);
    }

    private final class UnderTestBaseLocatorControllerTest extends BaseLocatorController {
        private UnderTestBaseLocatorControllerTest(final GameAvailabilityService gameAvailabilityService) {
            super(gameAvailabilityService);
        }
    }
}
