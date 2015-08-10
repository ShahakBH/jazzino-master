package com.yazino.web.controller;

import com.yazino.game.api.GameType;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.InviteFriendsTracking;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.ui.ModelMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class FacebookInviteFriendsControllerTest {

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private InvitationService invitationService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private SiteConfiguration siteConfiguration;
    @Mock
    private HierarchicalMessageSource resourceBundleMessageSource;
    @Mock
    private InviteFriendsTracking inviteFriendsTracking;

    private FacebookInviteFriendsController underTest;
    private ModelMap model;
    private LobbySession lobbySession;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final PlayerService playerService = mock(PlayerService.class);
        final PlayerProfileService playerProfileService = mock(PlayerProfileService.class);

        final GameTypeRepository gameTypeRepository = mock(GameTypeRepository.class);
        when(gameTypeRepository.getGameTypes()).thenReturn(Collections.singletonMap(
                "aGameType", new GameTypeInformation(
                        new GameType("aGameType", "aGameTypeName", Collections.<String>emptySet()), true)
        ));

        underTest = new FacebookInviteFriendsController(lobbySessionCache, invitationService,
                playerService, playerProfileService, gameTypeRepository,
                cookieHelper, siteConfiguration, resourceBundleMessageSource, inviteFriendsTracking);

        model = new ModelMap();

        lobbySession = new LobbySession(BigDecimal.valueOf(3141592), new BigDecimal(1), "", "", YAZINO, "", "", null, false, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
    }

    @Test
    public void inviteFriendsShouldDoNothingIfNoIdsPassed() {
        when(request.getParameterValues("ids[]")).thenReturn(null);

        final String retVal = underTest.invitationsToPrivateTableSent();

        assertEquals("privateTableFacebookFriendsInviteSent", retVal);
    }

    @Test
    public void inviteFriendsFromFaceBookShouldStoreParamsAsInviteEvents() {
        final String[] ids = {"12345", "54321"};
        when(request.getParameterValues("ids[]")).thenReturn(ids);

        final String retVal = underTest.invitationsToPrivateTableSent();

        assertEquals("privateTableFacebookFriendsInviteSent", retVal);

    }

    @Test
    public void friendsInvitedViaFacebook_shouldUseLoggedInUserAsIssuer() {
        final BigDecimal ISSUING_USER_ID = lobbySession.getPlayerId();
        final String RECIPIENT_IDENTIFIER = "RecipientIdentifier";
        when(request.getParameter("request_ids")).thenReturn(RECIPIENT_IDENTIFIER);

        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);

        underTest.friendsInvitedViaFacebook(request, null);

        verify(lobbySessionCache).getActiveSession(request);
        verifyInvitationSentFor(ISSUING_USER_ID, new String[]{RECIPIENT_IDENTIFIER});
    }

    @Test
    public void ifRequestIdsAreMissingThenNoInvitationsAreSent() {
        when(request.getParameter("request_ids")).thenReturn(null);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);

        underTest.friendsInvitedViaFacebook(request, null);

        verify(cookieHelper).getLastGameType((Cookie[]) any(), anyString());
        verify(lobbySessionCache).getActiveSession(request);
        verifyZeroInteractions(invitationService);
    }

    @Test
    public void friendsInvitedViaFacebook_shouldDevourAndLogExceptionsAndReturnCloserMask() {

        final String RECIPIENT_IDENTIFIER = "RecipientIdentifier";
        when(request.getParameterValues("ids[]")).thenReturn(new String[]{RECIPIENT_IDENTIFIER});
        doThrow(new RuntimeException("Example Exception")).when(invitationService).invitationSent(
                any(BigDecimal.class), anyString(), any(InvitationSource.class), any(DateTime.class), anyString(),
                anyString());

        final String viewName = underTest.friendsInvitedViaFacebook(request, null);

        assertEquals("partials/closeInviteFriendsDialog", viewName);
    }

    @Test
    public void friendsInvitedViaFacebook_shouldNotInvokeInvitationSentButShouldReturnCloserMask_noRecipients() {
        final String viewName = underTest.friendsInvitedViaFacebook(request, null);

        verifyZeroInteractions(invitationService);
        assertEquals("partials/closeInviteFriendsDialog", viewName);
    }

    @Test
    public void friendsInvitedViaFacebook_shouldInvokeInvitationSentAndReturnCloserMask_singleRecipient() {
        final BigDecimal ISSUING_USER_ID = lobbySession.getPlayerId();
        final String RECIPIENT_IDENTIFIER = "RecipientIdentifier";
        when(request.getParameter("request_ids")).thenReturn(RECIPIENT_IDENTIFIER);

        final String viewName = underTest.friendsInvitedViaFacebook(request, null);

        verifyInvitationSentFor(ISSUING_USER_ID, new String[]{RECIPIENT_IDENTIFIER});
        assertEquals("partials/closeInviteFriendsDialog", viewName);
    }

    @Test
    public void friendsInvitedViaFacebook_shouldInvokeInvitationSentAndReturnCloserMask_multipleRecipients() {
        final BigDecimal ISSUING_USER_ID = lobbySession.getPlayerId();
        final String RECIPIENT_1_IDENTIFIER = "Recipient1Identifier";
        final String RECIPIENT_2_IDENTIFIER = "Recipient2Identifier";
        when(request.getParameter("request_ids")).thenReturn(RECIPIENT_1_IDENTIFIER + "," + RECIPIENT_2_IDENTIFIER);

        final String viewName = underTest.friendsInvitedViaFacebook(request, null);

        verifyInvitationSentFor(ISSUING_USER_ID, new String[]{RECIPIENT_1_IDENTIFIER, RECIPIENT_2_IDENTIFIER});
        assertEquals("partials/closeInviteFriendsDialog", viewName);
    }

    @Test
    public void friendsInvitedViaFacebook_shouldLogGameAndSourceInformation() {
        // GIVEN the cookie helper returns a game name
        final String GAME_TYPE = "GAME_TYPE";
        given(cookieHelper.getLastGameType(any(Cookie[].class), eq(""))).willReturn(GAME_TYPE);
        given(cookieHelper.getScreenSource(any(Cookie[].class))).willReturn("OTHER_SOURCE", (String) null);
        given(siteConfiguration.getDefaultGameType()).willReturn("");

        // AND the request is containing a single identifier
        final BigDecimal ISSUING_USER_ID = lobbySession.getPlayerId();
        final String RECIPIENT_1_IDENTIFIER = "RecipientIdentifier1";
        given(request.getParameter("request_ids")).willReturn("RecipientIdentifier1,RecipientIdentifier2");

        // WHEN the invitation confirmation is required
        underTest.friendsInvitedViaFacebook(request, "source");
        underTest.friendsInvitedViaFacebook(request, null);
        underTest.friendsInvitedViaFacebook(request, null);

        // THEN the game name is correctly sent to the service
        verifyInvitationSentFor(ISSUING_USER_ID, RECIPIENT_1_IDENTIFIER, GAME_TYPE, "source");
        verifyInvitationSentFor(ISSUING_USER_ID, RECIPIENT_1_IDENTIFIER, GAME_TYPE, "OTHER_SOURCE");
        verifyInvitationSentFor(ISSUING_USER_ID, RECIPIENT_1_IDENTIFIER, GAME_TYPE, "FACEBOOK_POPUP");
    }

    @Test
    public void shouldReturnCorrectInvitationTextForGameType() {
        String source = "MY_SOURCE";
        String expectedMessage = "My really cool invite message";
        given(resourceBundleMessageSource.getMessage("invite." + source + ".text", null, null)).willReturn(
                expectedMessage);
        underTest.inviteFriendsViaFacebook(model, request, source, null);
        assertEquals(expectedMessage, model.get("inviteMessage"));
    }

    @Test
    public void shouldReturnCorrectInvitationTextForIngameGameType() {
        String source = "BLACKJACK_IN_GAME";
        String messageText = "BLACKJACK";
        String expectedMessage = "My really cool invite message";
        given(resourceBundleMessageSource.getMessage("invite." + messageText + ".text", null, null)).willReturn(
                expectedMessage);
        underTest.inviteFriendsViaFacebook(model, request, source, null);
        assertEquals(expectedMessage, model.get("inviteMessage"));
    }

    @Test
    public void shouldReturnDefaultInvitationTextForGameType() {
        String source = "MY_NONEXISTANT_SOURCE";
        String expectedMessage = "My really cool invite message";
        given(resourceBundleMessageSource.getMessage("invite.default.text", null, null)).willReturn(expectedMessage);
        underTest.inviteFriendsViaFacebook(model, request, source, null);
        assertEquals(expectedMessage, model.get("inviteMessage"));
    }

    @Test
    public void shouldReturnSourceWhenInivteFriendsCalled() {
        String source = "MY_NONEXISTANT_SOURCE";
        underTest.inviteFriendsViaFacebook(model, request, source, null);
        assertEquals(source, model.get("source"));
    }

    @Test
    public void shouldSetCorrectReferringPlayerId() {
        String source = "MY_NONEXISTANT_SOURCE";
        given(lobbySessionCache.getActiveSession(request)).willReturn(lobbySession);
        underTest.inviteFriendsViaFacebook(model, request, source, null);
        assertEquals(lobbySession.getPlayerId(), model.get("referringPlayerId"));
    }

    @Test
    public void inviteFriendsViaFacebook_shouldPropagateTargetFriendIdWhenPresent() {
        String source = "MY_NONEXISTANT_SOURCE";
        String targetFriendId = "aTargetFriendId";
        given(lobbySessionCache.getActiveSession(request)).willReturn(lobbySession);
        underTest.inviteFriendsViaFacebook(model, request, source, targetFriendId);
        assertEquals(targetFriendId, model.get("targetFriendId"));
    }

    @Test
    public void inviteFriendsViaFacebook_shouldNotPropagateTargetFriendIdWhenNotPresent() {
        String source = "MY_NONEXISTANT_SOURCE";
        given(lobbySessionCache.getActiveSession(request)).willReturn(lobbySession);
        underTest.inviteFriendsViaFacebook(model, request, source, null);
        assertNull(model.get("targetFriendId"));
    }

    private void verifyInvitationSentFor(final BigDecimal issuingUserId, final String[] recipientIdentifiers) {
        for (final String recipientIdentifier : recipientIdentifiers) {
            verifyInvitationSentFor(issuingUserId, recipientIdentifier, null, "FACEBOOK_POPUP");
        }
    }

    private void verifyInvitationSentFor(final BigDecimal issuingUserId, final String recipientIdentifier,
                                         final String gameType, final String source) {
        verify(invitationService).invitationSent(eq(issuingUserId), eq(recipientIdentifier),
                eq(InvitationSource.FACEBOOK), any(DateTime.class), eq(gameType), eq(source));
    }

    @Test
    public void friendsInvitedViaFacebookShouldBeTracked() {
        final BigDecimal ISSUING_USER_ID = lobbySession.getPlayerId();
        final String RECIPIENT_1_IDENTIFIER = "Recipient1Identifier";
        final String RECIPIENT_2_IDENTIFIER = "Recipient2Identifier";
        when(request.getParameter("request_ids")).thenReturn(RECIPIENT_1_IDENTIFIER + "," + RECIPIENT_2_IDENTIFIER);

        underTest.friendsInvitedViaFacebook(request, null);

        verify(inviteFriendsTracking).trackSuccessfulInviteFriends(ISSUING_USER_ID,
                InviteFriendsTracking.InvitationType.FACEBOOK, 2);
    }

    @Test
    public void friendsInvitedViaFacebookShouldNotBeTrackedForZeroInvitations() {
        when(request.getParameter("request_ids")).thenReturn(null);
        verifyNoMoreInteractions(inviteFriendsTracking);

        underTest.friendsInvitedViaFacebook(request, null);
    }

    @Test
    public void invitingFriendsToAPrivateTableShouldPopulateTheModel() {
        underTest.inviteFriendsToPrivateTable(request, response, model, "aGameType", "100", "aTableName");

        assertThat((String) model.get("privateTableGameName"), is(equalTo("aGameTypeName")));
        assertThat((BigDecimal) model.get("privateTableId"), is(equalTo(BigDecimal.valueOf(100))));
        assertThat((String) model.get("privateTableName"), is(equalTo("aTableName")));
        verifyZeroInteractions(response);
    }

    @Test
    public void invitingFriendsToAPrivateTableShouldReturnsABadRequestForAnInvalidTableId() throws IOException {
        final String viewName = underTest.inviteFriendsToPrivateTable(
                request, response, model, "aGameType", "100F", "aTableName");

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(viewName, is(nullValue()));
    }

    @Test
    public void invitingFriendsToAPrivateTableShouldReturnsTheAppropriateView() {
        final String viewName = underTest.inviteFriendsToPrivateTable(
                request, response, model, "aGameType", "100", "aTableName");

        assertThat(viewName, is(equalTo("privateTableFacebookFriendsInvite")));
    }
}
