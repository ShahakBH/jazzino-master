package com.yazino.web.controller.profile;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.invitation.InvitationQueryService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InvitationStatementControllerTest {

    public static final boolean NOT_PARTIAL = false;
    public static final boolean PARTIAL = true;
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;

    private LobbySessionCache lobbySessionCache;
    private LobbySession lobbySession;
    private InvitationQueryService invitationQueryService;
    private HttpServletRequest request;

    private InvitationStatementController underTest;
    private YazinoConfiguration yazinoConfiguration;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        lobbySession = mock(LobbySession.class);
        lobbySessionCache = mock(LobbySessionCache.class);
        invitationQueryService = mock(InvitationQueryService.class);
        yazinoConfiguration = mock(YazinoConfiguration.class);
        when(yazinoConfiguration.getBoolean(anyString())).thenReturn(true);
        underTest = new InvitationStatementController(lobbySessionCache, invitationQueryService, yazinoConfiguration);

        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
    }

    @Test
    public void testModelAndViewPointsToCorrectCurrentTabAndPage() {
        ModelAndView result = underTest.displayRootPage(NOT_PARTIAL, request);

        ProfileTestHelper.assertNonPartialLayout(result);
        ProfileTestHelper.assertSelectTabIs(result, "invitations");
    }

    @Test
    public void testModelContainsInviteeList() {
        ModelAndView result = underTest.displayRootPage(NOT_PARTIAL, request);

        assertNotNull(result.getModel().get("invitees"));
        assertTrue("invitees should be a collection", result.getModel().get("invitees") instanceof Collection);
    }

    @Test
    public void testModelContainsTabList() {
        ModelAndView result = underTest.displayRootPage(NOT_PARTIAL, request);

        ProfileTestHelper.assertFullListOfTabsInModel(result);
    }

    @Test
    public void shouldReturnCorrectViewForMainProfilePage() {
        ModelAndView result = underTest.displayRootPage(NOT_PARTIAL, request);

        assertMainProfilePageViewName(result);
    }

    @Test
    public void shouldReturnCorrectViewForMainProfilePageAsPartial() {
        ModelAndView result = underTest.displayRootPage(PARTIAL, request);
        
        assertMainProfilePagePartialViewName(result);
    }

    @Test
    public void shouldReturnModelContainingSummaryData() {
        ModelAndView result = underTest.displayRootPage(PARTIAL, request);

        assertTrue(result.getModel().containsKey("totalInvitesSent"));
        assertTrue(result.getModel().containsKey("totalChipsEarned"));
        assertTrue(result.getModel().containsKey("totalInvitesPending"));
        assertTrue(result.getModel().containsKey("totalInvitesAccepted"));
    }

    private void assertMainProfilePageViewName(ModelAndView actualModelAndView) {
        ProfileTestHelper.assertNonPartialLayout(actualModelAndView);
        ProfileTestHelper.assertSelectTabIs(actualModelAndView, "invitations");
    }

    private void assertMainProfilePagePartialViewName(ModelAndView actualModelAndView) {
        ProfileTestHelper.assertPartialLayout(actualModelAndView);
        ProfileTestHelper.assertSelectTabIs(actualModelAndView, "invitations");
    }

}
