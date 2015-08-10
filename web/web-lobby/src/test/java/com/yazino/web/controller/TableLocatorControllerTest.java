package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.table.*;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.form.TableLocatorForm;
import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.GameConfigurationRepository;
import com.yazino.web.service.TableLobbyService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ModelMap;
import com.yazino.game.api.GameType;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;

import static com.yazino.platform.Platform.WEB;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TableLocatorControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(243);
    private static final String GAME_TYPE = "BLACKJACK";
    private static final String VARIATION_NAME = "Atlantic City";
    private static final Partner PARTNER_ID = Partner.YAZINO;
    private static final String CLIENT_ID = "Red Blackjack";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final TableLobbyService tableLobbyService = mock(TableLobbyService.class);
    private final TableService tableService = mock(TableService.class);
    private final GameConfigurationRepository gameConfigurationRepository = mock(GameConfigurationRepository.class);
    private final TableLocatorForm form = new TableLocatorForm();
    private final LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private final CookieHelper cookieHelper = mock(CookieHelper.class);
    private final GameAvailabilityService gameAvailabilityService = mock(GameAvailabilityService.class);

    private final ModelMap modelMap = new ModelMap();
    private final SiteConfiguration siteConfiguration = new SiteConfiguration();
    private final FacebookConfiguration facebookConfiguration = new FacebookConfiguration();

    private final TableLocatorController underTest = new TableLocatorController(tableLobbyService, siteConfiguration, lobbySessionCache,
            tableService, cookieHelper, gameAvailabilityService, gameConfigurationRepository);

    @Before
    public void setUp() {
        siteConfiguration.setPartnerId(PARTNER_ID);

        final FacebookAppConfiguration facebookAppConfiguration = new FacebookAppConfiguration();
        facebookAppConfiguration.setGameType(GAME_TYPE);
        facebookAppConfiguration.setApiKey("anApiKey");
        facebookAppConfiguration.setApplicationId("anApplicationId");
        facebookConfiguration.setApplicationConfigs(asList(facebookAppConfiguration));

        when(cookieHelper.isOnCanvas(request, response)).thenReturn(false);

        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "test", "session", Partner.YAZINO, "pic", "email", null, false,
                WEB, AuthProvider.YAZINO));
        when(tableLobbyService.isGameTypeAvailable(GAME_TYPE)).thenReturn(true);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(new GameAvailability(GameAvailabilityService.Availability.AVAILABLE));
    }

    @Test
    public void findSimilarTableIdPassesTableIdAndUserToService() throws TableException {
        final BigDecimal tableId = BigDecimal.valueOf(69);
        final BigDecimal newTableId = BigDecimal.valueOf(343);

        when(tableLobbyService.findOrCreateSimilarTable(tableId, PLAYER_ID)).thenReturn(summaryFor(newTableId));

        assertEquals("playGame", underTest.findSimilarTableId(tableId.toPlainString(), modelMap, request, response));

        verify(tableLobbyService).findOrCreateSimilarTable(tableId, PLAYER_ID);
    }

    @Test
    public void findSimilarTableIdReturnsABadRequestForAnInvalidTableID() throws TableException, IOException {
        assertThat(underTest.findSimilarTableId("invalidId", modelMap, request, response), is(nullValue()));
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void findSimilarTableIdRedirectsToViewOnError() throws TableException {
        final BigDecimal tableId = BigDecimal.valueOf(69);
        final String errorMessage = "gerbils, gerbils!";

        when(tableLobbyService.findOrCreateSimilarTable(tableId, PLAYER_ID)).thenThrow(new RuntimeException(errorMessage));

        assertEquals("tableLocation", underTest.findSimilarTableId(tableId.toPlainString(), modelMap, request, response));

        assertEquals(errorMessage, modelMap.get("reason"));

        verify(tableLobbyService).findOrCreateSimilarTable(tableId, PLAYER_ID);
    }

    @Test
    public void findTableIdSuccessfullyUsesTableLobbyServiceAndFindsTableId() throws TableException, IOException {
        form.setClientId(CLIENT_ID);
        form.setGameType(GAME_TYPE);
        form.setVariationName(VARIATION_NAME);

        final BigDecimal tableId = BigDecimal.valueOf(69);

        when(tableService.findSummaryById(tableId)).thenReturn(summaryFor(tableId));
        when(tableLobbyService.isGameTypeAvailable(GAME_TYPE)).thenReturn(true);
        when(tableLobbyService.findOrCreateTableByGameTypeAndVariation(GAME_TYPE, VARIATION_NAME, CLIENT_ID, PLAYER_ID, Collections.<String>emptySet()))
                .thenReturn(summaryFor(tableId));

        String goToView = underTest.findTableId(form, modelMap, request, response);
        assertThat(goToView, is(equalTo("playGame")));
    }

    @Test
    public void findTableIdFailsBecauseOfFormValidationError() throws IOException {
        form.setClientId(CLIENT_ID);
        form.setVariationName(VARIATION_NAME);

        String goToView = underTest.findTableId(form, modelMap, request, response);
        assertEquals("tableLocation", goToView);

        verifyZeroInteractions(response);
    }

    @Test
    public void findTableIdFailsBecauseOfTableLobbyServiceException() throws TableException {
        form.setClientId(CLIENT_ID);
        form.setGameType(GAME_TYPE);
        form.setVariationName(VARIATION_NAME);


        TableException tableException = new TableException(TableOperationResult.FAILURE);
        when(tableLobbyService.findOrCreateTableByGameTypeAndVariation(GAME_TYPE, VARIATION_NAME, CLIENT_ID, PLAYER_ID, Collections.<String>emptySet())).thenThrow(tableException);

        String goToView = underTest.findTableId(form, modelMap, request, response);
        assertEquals(TableOperationResult.FAILURE.name(), modelMap.get("reason"));
        assertEquals("tableLocation", goToView);
    }

    @Test
    public void launchTableFailsWhenTableDetailsCannotBeRetrieved() throws Exception {
        final String view = underTest.launchTableWithId(request, response, modelMap, BigDecimal.ONE.toPlainString());

        assertThat(view, is(nullValue()));
        verify(response).sendError(401, "No table detail found for tableId: 1");
    }

    @Test
    public void launchTableIdReturnsABadRequestForAnInvalidTableID() throws TableException, IOException {
        assertThat(underTest.launchTableWithId(request, response, modelMap, "invalidId"), is(nullValue()));
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void launchTableForwardsToView() throws Exception {
        final BigDecimal tableId = BigDecimal.ONE;
        when(tableService.findSummaryById(tableId)).thenReturn(summaryFor(tableId));

        final String view = underTest.launchTableWithId(request, response, modelMap, tableId.toPlainString());

        assertThat(view, is(equalTo("playGame")));
        assertThat((BigDecimal) modelMap.get("tableId"), is(equalTo(tableId)));
    }

    private TableSummary summaryFor(final BigDecimal tableId) {
        final GameType gameType = new GameType(GAME_TYPE, GAME_TYPE, Collections.<String>emptySet());
        return new TableSummary(tableId, "aTableName", TableStatus.open, "aGameTypeId", gameType, null,
                "aClient", "aClientFileName", "aTemplateName", null, Collections.<BigDecimal>emptySet(), Collections.<String>emptySet());
    }
}
