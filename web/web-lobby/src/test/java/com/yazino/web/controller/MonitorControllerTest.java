package com.yazino.web.controller;

import com.yazino.game.api.GameType;
import com.yazino.platform.metrics.MetricsService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.session.SessionService;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.table.TableSearchOption;
import com.yazino.platform.table.TableService;
import com.yazino.platform.table.TableType;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MonitorControllerTest {
    private static final int NUMBER_OF_PLAYERS = 134;
    private static final int NUMBER_OF_USER_PROFILES = 17;
    private static final int NUMBER_OF_BROKEN_TABLES = 7;
    private static final int PLAYER_FETCH_ERROR = -1;

    @Mock
    private TableService tableService;
    @Mock
    private SessionService sessionService;
    @Mock
    private PlayerProfileService userProfileService;
    @Mock
    private HttpServletResponse response;
    @Mock
    private MetricsService metricsService;
    @Mock
    private WebApiResponses webApiResponses;

    private MonitorController underTest;

    @Before
    public void setUp() throws IOException {
        underTest = new MonitorController(tableService, sessionService, userProfileService, metricsService, webApiResponses);

        when(sessionService.countSessions(true)).thenReturn(NUMBER_OF_PLAYERS);

        final Set<GameTypeInformation> gameTypeInformation =
                newHashSet(new GameTypeInformation(new GameType("anAvailableGameType", "aGT", Collections.<String>emptySet()), true),
                        new GameTypeInformation(new GameType("anUnavailableGameType", "uGT", Collections.<String>emptySet()), false));
        when(tableService.getGameTypes()).thenReturn(gameTypeInformation);
        when(tableService.countByType(TableType.ALL, TableSearchOption.IN_ERROR_STATE)).thenReturn(
                NUMBER_OF_BROKEN_TABLES);

        when(userProfileService.count()).thenReturn(NUMBER_OF_USER_PROFILES);
    }

    @Test
    public void metricsShouldReturnABadRequestOnAnInvalidRange() throws IOException {
        underTest.showMetrics(response, "boo");

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid range: boo");
        verifyNoMoreInteractions(webApiResponses);
    }

    @Test
    public void metricsShouldReturnABadRequestOnAnMissingRange() throws IOException {
        underTest.showMetrics(response, null);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid range: null");
        verifyNoMoreInteractions(webApiResponses);
    }

    @Test
    public void metricsShouldTransformTheRequestedRangeToTheRangeEnum() throws IOException {
        underTest.showMetrics(response, "fifteen_minUtes");

        verify(metricsService).getMeters(MetricsService.Range.FIFTEEN_MINUTES);
    }

    @Test
    public void metricsShouldReturnTheResultingMapAsJSON() throws IOException {
        final HashMap<String, BigDecimal> resultMap = new HashMap<>();
        when(metricsService.getMeters(MetricsService.Range.MEAN)).thenReturn(resultMap);

        underTest.showMetrics(response, "mean");

        verify(webApiResponses).writeOk(response, resultMap);
        verifyNoMoreInteractions(webApiResponses);
    }

    @Test
    public void metricsShouldCatchExceptionsFromTheMetricsServerAndReturnAnInternalServerError() throws IOException {
        when(metricsService.getMeters(MetricsService.Range.ONE_MINUTE)).thenThrow(new RuntimeException("aTestException"));

        underTest.showMetrics(response, "one_minute");

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to return meters for range one_minute");
        verifyNoMoreInteractions(webApiResponses);
    }

    @SuppressWarnings({"NullableProblems"})
    @Test(expected = NullPointerException.class)
    public void theHandlerShouldRejectANullResponse() throws IOException {
        underTest.listMonitoringInformation(null);
    }

    @Test
    public void theHandlerShouldReturnAValidJSONOutput() throws IOException {
        underTest.listMonitoringInformation(response);

        verify(webApiResponses).writeOk(eq(response), anyObject());
    }

    @Test
    public void theResultShouldHaveACountOfCurrentPlayers() throws IOException {
        underTest.listMonitoringInformation(response);

        final ArgumentCaptor<Map> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), responseCaptor.capture());
        assertThat((Integer) responseCaptor.getValue().get("players"), is(equalTo(NUMBER_OF_PLAYERS)));
    }

    @Test
    public void theResultShouldHaveACountOfCurrentUserProfiles() throws IOException {
        underTest.listMonitoringInformation(response);

        final ArgumentCaptor<Map> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), responseCaptor.capture());
        assertThat((Integer) responseCaptor.getValue().get("userProfiles"), is(equalTo(NUMBER_OF_USER_PROFILES)));
    }

    @Test
    public void whenTheSessionCountFailsTheCountShouldBeReturnedAsNegativeOne() throws IOException {
        reset(sessionService);
        when(sessionService.countSessions(true)).thenThrow(new IllegalStateException("aTestException"));

        underTest.listMonitoringInformation(response);

        final ArgumentCaptor<Map> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), responseCaptor.capture());
        assertThat((Integer) responseCaptor.getValue().get("players"), is(equalTo(PLAYER_FETCH_ERROR)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void theResultShouldShowTheAvailabilityOfEachGameType() throws IOException {
        underTest.listMonitoringInformation(response);

        final ArgumentCaptor<Map> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), responseCaptor.capture());
        final Map<String, Object> gameTypes = (Map<String, Object>) responseCaptor.getValue().get("gameTypes");
        assertThat((Boolean) gameTypes.get("anAvailableGameType"), is(true));
        assertThat((Boolean) gameTypes.get("anUnavailableGameType"), is(false));
    }

    @Test
    public void whenGameTypeRetrievalFailsTheResultShouldContainAnEmptyList() throws IOException {
        reset(tableService);
        when(tableService.getGameTypes()).thenThrow(new IllegalStateException("aTestException"));

        underTest.listMonitoringInformation(response);

        final ArgumentCaptor<Map> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), responseCaptor.capture());
        assertThat(((Map) responseCaptor.getValue().get("gameTypes")).size(), is(equalTo(0)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void theResultShouldShowTheNumberOfBrokenTables() throws IOException {
        underTest.listMonitoringInformation(response);

        final ArgumentCaptor<Map> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), responseCaptor.capture());
        final Map<String, Object> tables = (Map<String, Object>) responseCaptor.getValue().get("tables");
        assertThat((Integer) tables.get("inErrorState"), is(equalTo(NUMBER_OF_BROKEN_TABLES)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whenBrokenTablesRetrievalFailsTheResultShouldContainAnEmptyList() throws IOException {
        reset(tableService);
        when(tableService.countByType(TableType.ALL, TableSearchOption.IN_ERROR_STATE)).thenThrow(
                new RuntimeException("aTestException"));

        underTest.listMonitoringInformation(response);

        final ArgumentCaptor<Map> responseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), responseCaptor.capture());
        assertThat(((Map) responseCaptor.getValue().get("tables")).size(), is(equalTo(0)));
    }

}
