package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.game.api.GameType;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.table.TableException;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.table.TableSummary;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.form.TableLocatorForm;
import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.TableLobbyService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@SuppressWarnings("unchecked")
public class MobileTableLocatorControllerTest {

    private static final String GAME_TYPE = "DUMMY_GAME";
    private final TableLobbyService tableLobbyService = mock(TableLobbyService.class);
    private final LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private final WebApiResponses responseWriter = mock(WebApiResponses.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final TableLocatorForm form = new TableLocatorForm();
    private final SiteConfiguration siteConfiguration = new SiteConfiguration();
    private final GameAvailabilityService gameAvailabilityService = mock(GameAvailabilityService.class);

    private final YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);
    private final MobileTableLocatorController controller = new MobileTableLocatorController(responseWriter, tableLobbyService, siteConfiguration, lobbySessionCache, gameAvailabilityService, yazinoConfiguration);

    @Before
    public void setup() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(new LobbySession(BigDecimal.valueOf(3141592), BigDecimal.ONE, "P", "1", Partner.YAZINO, "1", "1", null, true,
                Platform.IOS, AuthProvider.YAZINO));
        when(tableLobbyService.isGameTypeAvailable("DUMMY_GAME")).thenReturn(true);
        form.setClientId("DUMMY_CLIENT");
        form.setGameType(GAME_TYPE);
        form.setVariationName("DUMMY_VARIATION");
    }

    @Test
    public void shouldUseResponseWriter() throws Exception {
        controller.findTableId(form, request, response);
        verify(responseWriter).writeOk(eq(response), anyObject());
    }

    @Test
    public void shouldWriteAnErrorResponseWhenFormIsNotComplete() throws Exception {
        ArgumentCaptor<MobileTableLocatorController.TableLocatorResponse> locatorResponseCaptor = ArgumentCaptor.forClass(MobileTableLocatorController.TableLocatorResponse.class);
        form.setGameType(null);
        controller.findTableId(form, request, response);
        verify(responseWriter).writeOk(eq(response), locatorResponseCaptor.capture());
        MobileTableLocatorController.TableLocatorResponse locatorResponse = locatorResponseCaptor.getValue();
        assertEquals(TableLocatorForm.GAME_TYPE_BLANK, locatorResponse.getError());
    }

    @Test
    public void shouldReturnAnErrorResponseWhenUserIsNotLoggedIn() throws Exception {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        ArgumentCaptor<MobileTableLocatorController.TableLocatorResponse> locatorResponseCaptor = ArgumentCaptor.forClass(MobileTableLocatorController.TableLocatorResponse.class);
        controller.findTableId(form, request, response);
        verify(responseWriter).writeOk(eq(response), locatorResponseCaptor.capture());
        MobileTableLocatorController.TableLocatorResponse locatorResponse = locatorResponseCaptor.getValue();
        assertEquals(MobileTableLocatorController.ERROR_NO_LOBBY_SESSION, locatorResponse.getError());
    }

    @Test
    public void shouldReturnAnErrorResponseWhenGameIsNotAvailable() throws Exception {
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.DISABLED);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);
        ArgumentCaptor<MobileTableLocatorController.TableLocatorResponse> locatorResponseCaptor = ArgumentCaptor.forClass(MobileTableLocatorController.TableLocatorResponse.class);
        controller.findTableId(form, request, response);
        verify(responseWriter).writeOk(eq(response), locatorResponseCaptor.capture());
        MobileTableLocatorController.TableLocatorResponse locatorResponse = locatorResponseCaptor.getValue();
        assertEquals(MobileTableLocatorController.ERROR_GAME_DISABLED, locatorResponse.getError());
    }

    @Test
    public void shouldReturnASuccessfulResponseWhenAllocationSucceeds() throws Exception {
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.AVAILABLE);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);
        BigDecimal tableId = new BigDecimal("90");
        TableSummary summary = new TableSummary(tableId, "FOO", TableStatus.open, "aGameTypeId", new GameType("123", "TEST", new HashSet<String>()),
                BigDecimal.ONE, "", "", "", "", new HashSet<BigDecimal>(), Collections.<String>emptySet());
        when(tableLobbyService.findOrCreateTableByGameTypeAndVariation(anyString(), anyString(), anyString(), any(BigDecimal.class), any(Set.class))).thenReturn(summary);

        ArgumentCaptor<MobileTableLocatorController.TableLocatorResponse> locatorResponseCaptor = ArgumentCaptor.forClass(MobileTableLocatorController.TableLocatorResponse.class);
        controller.findTableId(form, request, response);
        verify(responseWriter).writeOk(eq(response), locatorResponseCaptor.capture());
        MobileTableLocatorController.TableLocatorResponse locatorResponse = locatorResponseCaptor.getValue();
        assertEquals(tableId, locatorResponse.getTableId());
        assertNull(locatorResponse.getError());
    }

    @Test
    public void shouldIncludeAvailabilityOfGameTypeInSuccessResponse() throws IOException, TableException {
        BigDecimal tableId = new BigDecimal("90");
        TableSummary summary = new TableSummary(tableId, "FOO", TableStatus.open, "aGameTypeId", new GameType("123", "TEST", new HashSet<String>()),
                BigDecimal.ONE, "", "", "", "", new HashSet<BigDecimal>(), Collections.<String>emptySet());
        when(tableLobbyService.findOrCreateTableByGameTypeAndVariation(anyString(), anyString(), anyString(), any(BigDecimal.class), any(Set.class))).thenReturn(summary);
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.AVAILABLE);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);
        ArgumentCaptor<MobileTableLocatorController.TableLocatorResponse> locatorResponseCaptor = ArgumentCaptor.forClass(MobileTableLocatorController.TableLocatorResponse.class);

        controller.findTableId(form, request, response);

        verify(responseWriter).writeOk(eq(response), locatorResponseCaptor.capture());
        MobileTableLocatorController.TableLocatorResponse locatorResponse = locatorResponseCaptor.getValue();
        assertThat(locatorResponse.getAvailability(), equalTo(GameAvailabilityService.Availability.AVAILABLE));
    }

    @Test
    public void shouldSetMaintenanceCountdownInSuccessResponseWhenMaintenanceScheduledForSpecifiedGame() throws TableException, IOException {
        Long expectedCountdown = 10L;
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED, expectedCountdown);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);

        BigDecimal tableId = new BigDecimal("90");
        TableSummary summary = new TableSummary(tableId, "FOO", TableStatus.open, "aGameTypeId", new GameType("123", "TEST", new HashSet<String>()),
                BigDecimal.ONE, "", "", "", "", new HashSet<BigDecimal>(), Collections.<String>emptySet());
        when(tableLobbyService.findOrCreateTableByGameTypeAndVariation(anyString(), anyString(), anyString(), any(BigDecimal.class), any(Set.class))).thenReturn(summary);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);
        ArgumentCaptor<MobileTableLocatorController.TableLocatorResponse> locatorResponseCaptor = ArgumentCaptor.forClass(MobileTableLocatorController.TableLocatorResponse.class);

        controller.findTableId(form, request, response);

        verify(responseWriter).writeOk(eq(response), locatorResponseCaptor.capture());
        MobileTableLocatorController.TableLocatorResponse locatorResponse = locatorResponseCaptor.getValue();
        assertThat(locatorResponse.getAvailability(), equalTo(GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED));
        assertThat(locatorResponse.getMaintenanceStartsAtMillis(), equalTo(expectedCountdown));
    }

    @Test
    public void shouldNotSetMaintenanceCountdownInSuccessResponseWhenNoMaintenanceScheduledForSpecifiedGame() throws TableException, IOException {
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.AVAILABLE);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);

        BigDecimal tableId = new BigDecimal("90");
        TableSummary summary = new TableSummary(tableId, "FOO", TableStatus.open, "aGameTypeId", new GameType("123", "TEST", new HashSet<String>()),
                BigDecimal.ONE, "", "", "", "", new HashSet<BigDecimal>(), Collections.<String>emptySet());
        when(tableLobbyService.findOrCreateTableByGameTypeAndVariation(anyString(), anyString(), anyString(), any(BigDecimal.class), any(Set.class))).thenReturn(summary);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);
        ArgumentCaptor<MobileTableLocatorController.TableLocatorResponse> locatorResponseCaptor = ArgumentCaptor.forClass(MobileTableLocatorController.TableLocatorResponse.class);

        controller.findTableId(form, request, response);

        verify(responseWriter).writeOk(eq(response), locatorResponseCaptor.capture());
        MobileTableLocatorController.TableLocatorResponse locatorResponse = locatorResponseCaptor.getValue();
        assertThat(locatorResponse.getAvailability(), equalTo(GameAvailabilityService.Availability.AVAILABLE));
        assertThat(locatorResponse.getMaintenanceStartsAtMillis(), equalTo(null));
    }

    @Test
    public void shouldAllowMappingOfVariationNames() throws IOException, TableException {
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.AVAILABLE);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);

        when(yazinoConfiguration.containsKey("table-locator.mappings.Slots_Not_Slow_For_Level_6_")).thenReturn(true);
        when(yazinoConfiguration.getString("table-locator.mappings.Slots_Not_Slow_For_Level_6_")).thenReturn("Slots Low");
        final TableLocatorForm form = new TableLocatorForm();
        form.setGameType(GAME_TYPE);
        form.setClientId("aclient");
        form.setVariationName("Slots Not Slow For Level 6+");
        controller.findTableId(form, request, response);
        verify(tableLobbyService).findOrCreateTableByGameTypeAndVariation(eq(GAME_TYPE), eq("Slots Low"), anyString(), eq(BigDecimal.ONE), any(Set.class));
    }

}
