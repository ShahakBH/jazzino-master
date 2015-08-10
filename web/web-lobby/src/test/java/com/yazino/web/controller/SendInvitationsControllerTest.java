package com.yazino.web.controller;

import com.yazino.email.EmailException;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.ApplicationInformation;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.InvitationLobbyService;
import com.yazino.web.service.InvitationSendingResult;
import com.yazino.web.service.InviteFriendsTracking;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Collections;

import static com.yazino.platform.Platform.WEB;
import static org.apache.commons.lang.StringUtils.join;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendInvitationsControllerTest {
    private static final String USER_IP_ADDRESS = "192.168.1.1";
    private static final String INVITATION_MESSAGE = "Nice knowing you sukka";
    private static final String GAME_TYPE = "aGameType";
    private static final String SCREEN_SOURCE = "FB_SCREEN";
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String RECIPIENT_ID = "recipient@example.com";
    private static final String[] EMAIL_ADDRESSES
            = {"example1@example.com", "example2@example.com", "example3@example.com"};
    private static final ApplicationInformation APP_INFO = new ApplicationInformation(GAME_TYPE, Partner.YAZINO, Platform.WEB);

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private InvitationService invitationService;
    @Mock
    private SiteConfiguration siteConfiguration;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private PlayerProfileService profileService;
    @Mock
    private InviteFriendsTracking inviteFriendsTracking;
    @Mock
    private InvitationLobbyService invitationLobbyService;

    private final PlayerProfile userProfile = new PlayerProfile(PLAYER_ID, "test@test.com",
            "Test Display", "Test Name", Gender.MALE, "UK", "firstName", "lastName", new DateTime(1970, 3, 3, 0, 0, 0, 0), null,
            "YAZINO", "rpxProvider", null, true);
    private final LobbySession lobbySession = new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "", "", Partner.YAZINO,
            null, null, null, false, WEB, AuthProvider.YAZINO);

    private SendInvitationsController underTest;

    @Before
    public void setup() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(profileService.findByPlayerId(PLAYER_ID)).thenReturn(userProfile);
        when(response.getWriter()).thenReturn(new PrintWriter(new ByteArrayOutputStream()));
        when(invitationLobbyService.sendInvitations(any(BigDecimal.class), any(ApplicationInformation.class), anyString(), anyString(),
                any(String[].class), anyBoolean(), eq(USER_IP_ADDRESS))).thenReturn(
                new InvitationSendingResult(0, Collections.<InvitationSendingResult.Rejection>emptySet()));
        when(request.getRemoteAddr()).thenReturn(USER_IP_ADDRESS);
        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(siteConfiguration.getDefaultGameType()).thenReturn("defaultGameType");

        final CookieHelper cookieHelper = mock(CookieHelper.class);
        when(cookieHelper.getLastGameType((Cookie[]) anyObject(), eq("defaultGameType"))).thenReturn(GAME_TYPE);

        underTest = new SendInvitationsController(
                lobbySessionCache, siteConfiguration, invitationService,
                inviteFriendsTracking, invitationLobbyService);
        underTest.setCookieHelper(cookieHelper);
    }

    @Test
    public void emailInvitations_shouldSplitEmailAddressesAroundCommas() throws Exception {
        underTest.emailInvitations(join(EMAIL_ADDRESSES, ","), SCREEN_SOURCE, INVITATION_MESSAGE, request);

        verify(invitationLobbyService).sendInvitations(
                PLAYER_ID, APP_INFO, SCREEN_SOURCE, INVITATION_MESSAGE, EMAIL_ADDRESSES, false, USER_IP_ADDRESS);
    }

    @Test
    public void emailInvitations_shouldTrimEmailAddresses_single() throws Exception {
        underTest.emailInvitations(" address1@example.com ", SCREEN_SOURCE, INVITATION_MESSAGE, request);

        verify(invitationLobbyService).sendInvitations(
                PLAYER_ID, APP_INFO, SCREEN_SOURCE, INVITATION_MESSAGE, new String[]{"address1@example.com"}, false, USER_IP_ADDRESS);
    }

    @Test
    public void emailInvitations_shouldTrimEmailAddresses_multiple() throws Exception {
        underTest.emailInvitations(join(EMAIL_ADDRESSES, " , "), SCREEN_SOURCE, INVITATION_MESSAGE, request);

        verify(invitationLobbyService).sendInvitations(
                PLAYER_ID, APP_INFO, SCREEN_SOURCE, INVITATION_MESSAGE, EMAIL_ADDRESSES, false, USER_IP_ADDRESS);
    }

    @Test
    public void emailInvitations_shouldReturnInvitationSendingResult() throws Exception {
        InvitationSendingResult expectedResult = new InvitationSendingResult(3, Collections.<InvitationSendingResult.Rejection>emptySet());
        when(invitationLobbyService
                .sendInvitations(PLAYER_ID, APP_INFO, SCREEN_SOURCE, INVITATION_MESSAGE, EMAIL_ADDRESSES, false, USER_IP_ADDRESS))
                .thenReturn(expectedResult);

        InvitationSendingResult actualResult = underTest.emailInvitations(
                join(EMAIL_ADDRESSES, ","), SCREEN_SOURCE, INVITATION_MESSAGE, request);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void emailInvitations_shouldTrackEmailInvitations() throws Exception {
        when(invitationLobbyService.sendInvitations(PLAYER_ID, APP_INFO, SCREEN_SOURCE, INVITATION_MESSAGE,
                new String[]{"example1@example.com", "example2@example.com", "example3@example.com"}, false, USER_IP_ADDRESS)).thenReturn(new
                InvitationSendingResult(3, Collections.<InvitationSendingResult.Rejection>emptySet()));

        underTest.emailInvitations(join(EMAIL_ADDRESSES, ","), SCREEN_SOURCE, INVITATION_MESSAGE, request);

        verify(inviteFriendsTracking).trackSuccessfulInviteFriends(
                PLAYER_ID, InviteFriendsTracking.InvitationType.EMAIL, 3);
    }

    @Test
    public void emailInvitationReminder_shouldEmailReminderToInvitee() throws EmailException, IOException {
        underTest.emailInvitationReminder(RECIPIENT_ID, request, response);

        verify(invitationLobbyService).sendInvitationReminders(PLAYER_ID, new String[]{RECIPIENT_ID}, GAME_TYPE, USER_IP_ADDRESS);
    }

    @Test(expected = IllegalStateException.class)
    public void emailInvitationReminder_shouldThrowIllegalStateExceptionWhenNoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        underTest.emailInvitationReminder(RECIPIENT_ID, request, response);

        verify(invitationService).invitationSent(
                eq(PLAYER_ID),
                eq(RECIPIENT_ID),
                eq(InvitationSource.EMAIL),
                any(DateTime.class),
                any(String.class),
                any(String.class));
    }
}
