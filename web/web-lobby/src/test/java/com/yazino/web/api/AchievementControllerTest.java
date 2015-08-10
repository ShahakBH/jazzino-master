package com.yazino.web.api;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PlayerTrophyService;
import com.yazino.platform.community.TrophyCabinet;
import com.yazino.platform.community.TrophySummary;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AchievementControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1000);

    @Mock
    private PlayerTrophyService playerTrophyService;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private AchievementController underTest;

    @Before
    public void setUp() {
        underTest = new AchievementController(playerTrophyService, lobbySessionCache, webApiResponses, yazinoConfiguration);

        when(yazinoConfiguration.getList("strata.server.lobby.medals", Collections.emptyList()))
                .thenReturn(asList((Object) "medal1", "medal2", "medal3"));
        when(yazinoConfiguration.getList("strata.server.lobby.trophy", Collections.emptyList()))
                .thenReturn(asList((Object) "trophy1"));

        when(lobbySessionCache.getActiveSession(request)).thenReturn(aSession());
        when(playerTrophyService.findTrophyCabinetForPlayer("aGameType", PLAYER_ID, asList("medal1", "medal2", "medal3", "trophy1")))
                .thenReturn(aTrophyCabinet());
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullPlayerTrophyService() {
        new AchievementController(null, lobbySessionCache, webApiResponses, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullLobbySessionCache() {
        new AchievementController(playerTrophyService, null, webApiResponses, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullWebApiResponse() {
        new AchievementController(playerTrophyService, lobbySessionCache, null, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullYazinoConfiguration() {
        new AchievementController(playerTrophyService, lobbySessionCache, webApiResponses, null);
    }

    @Test
    public void trophyCabinetSummaryShouldReturnUnauthorisedWhenNoLobbySessionIsPresent() throws IOException {
        reset(lobbySessionCache);

        underTest.trophyCabinetSummary("aGameType", request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "No session");
    }

    @Test
    public void trophyCabinetSummaryShouldReturnASummaryOfAllMedalsAndTrophies() throws IOException {
        underTest.trophyCabinetSummary("aGameType", request, response);

        verify(webApiResponses).writeOk(response, aSummary());
    }

    @Test
    public void trophyCabinetSummaryShouldReturnAnInternalServerErrorOnAnExceptionOutsideOfThePlayerTrophyService() throws IOException {
        reset(lobbySessionCache);
        when(lobbySessionCache.getActiveSession(request)).thenThrow(new IllegalStateException("aTestException"));

        underTest.trophyCabinetSummary("aGameType", request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to summarise trophy cabinet summary for game type aGameType and player null");
    }

    @Test
    public void trophyCabinetSummaryShouldReturnAnEmptySummaryOnAnExceptionFromThePlayerTrophyService() throws IOException {
        reset(playerTrophyService);
        when(playerTrophyService.findTrophyCabinetForPlayer("aGameType", PLAYER_ID, asList("medal1", "medal2", "medal3", "trophy1")))
                .thenThrow(new IllegalStateException("aTestException"));

        underTest.trophyCabinetSummary("aGameType", request, response);

        verify(webApiResponses).writeOk(response, anEmptySummary());
    }

    @Test
    public void trophyCabinetSummaryShouldPopulateMissingTrophiesInTheSummary() throws IOException {
        reset(playerTrophyService);
        when(playerTrophyService.findTrophyCabinetForPlayer("aGameType", PLAYER_ID, asList("medal1", "medal2", "medal3", "trophy1")))
                .thenReturn(aPartialTrophyCabinet());

        underTest.trophyCabinetSummary("aGameType", request, response);

        verify(webApiResponses).writeOk(response, aPartialSummary());
    }

    private TrophyCabinet aTrophyCabinet() {
        final TrophyCabinet trophyCabinet = new TrophyCabinet();
        trophyCabinet.addTrophySummary(new TrophySummary("medal1", "image1", new ParameterisedMessage("message1"), 2));
        trophyCabinet.addTrophySummary(new TrophySummary("medal2", "image2", new ParameterisedMessage("message2"), 4));
        trophyCabinet.addTrophySummary(new TrophySummary("medal3", "image3", new ParameterisedMessage("message3"), 8));
        trophyCabinet.addTrophySummary(new TrophySummary("trophy1", "imaget1", new ParameterisedMessage("messaget1"), 9));
        return trophyCabinet;
    }

    private TrophyCabinet aPartialTrophyCabinet() {
        final TrophyCabinet trophyCabinet = new TrophyCabinet();
        trophyCabinet.addTrophySummary(new TrophySummary("medal1", "image1", new ParameterisedMessage("message1"), 2));
        trophyCabinet.addTrophySummary(new TrophySummary("medal3", "image3", new ParameterisedMessage("message3"), 8));
        return trophyCabinet;
    }

    private Map<String, Object> aSummary() {
        final Map<String, Object> summary = new HashMap<>();
        summary.put("medal1", 2);
        summary.put("medal2", 4);
        summary.put("medal3", 8);
        summary.put("trophy1", 9);
        return summary;
    }

    private Map<String, Object> aPartialSummary() {
        final Map<String, Object> summary = new HashMap<>();
        summary.put("medal1", 2);
        summary.put("medal2", 0);
        summary.put("medal3", 8);
        summary.put("trophy1", 0);
        return summary;
    }

    private Map<String, Object> anEmptySummary() {
        final Map<String, Object> summary = new HashMap<>();
        summary.put("medal1", 0);
        summary.put("medal2", 0);
        summary.put("medal3", 0);
        summary.put("trophy1", 0);
        return summary;
    }

    private LobbySession aSession() {
        return new LobbySession(BigDecimal.ONE, PLAYER_ID, "aPlayerName", "aSessionKey", Partner.YAZINO,
                "aPictureUrl", "anEmail", null, false, Platform.WEB, AuthProvider.YAZINO);
    }

}
