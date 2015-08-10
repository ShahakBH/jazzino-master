package com.yazino.web.controller.social;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.ApplicationInformation;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.service.InvitationLobbyService;
import com.yazino.web.service.InvitationSendingResult;
import com.yazino.web.service.InviteFriendsTracking;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.WebApiResponses;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yazino.web.service.InviteFriendsTracking.InvitationType.EMAIL;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InvitationControllerTest extends AbstractSocialFlowTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String GAME_TYPE = "gameType";
    private static final ApplicationInformation APP_INFO = new ApplicationInformation(GAME_TYPE, Partner.YAZINO, Platform.AMAZON);
    private static final String SOURCE = "source";
    private static String PRE_SEND_HEADER = "preSend";
    private static String POST_SEND_HEADER = "postSend";
    private static final String PAGE_TYPE = "invitation";
    private static final String SEND_INVITE_CTA_TEXT = "Send Invite";
    private static final String USER_IP_ADDRESS = "192.168.1.1";
    private static final ResponseHelper.ProviderVO gmailProvider = new ResponseHelper.ProviderVO(
            "gmail", "gmail", "GMail Contacts", false, true);

    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private InvitationLobbyService invitationLobbyService;
    @Mock
    private InviteFriendsTracking tracking;
    @Mock
    private HierarchicalMessageSource messageSource;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private WebApiResponses responseHelper;


    private InvitationController underTest;

    @Before
    public void setUp() {
        final GameTypeResolver gameTypeResolver = mock(GameTypeResolver.class);
        final LobbySession lobbySession = new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "p",
                "psk", Partner.YAZINO, "pic", "email", null, false, Platform.WEB, AuthProvider.YAZINO);

        when(request.getRemoteAddr()).thenReturn(USER_IP_ADDRESS);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(gameTypeResolver.appInfoFor(request, response, lobbySession)).thenReturn(APP_INFO);
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(GAME_TYPE);

        underTest = new InvitationController(lobbySessionCache, invitationLobbyService,
                tracking, playerProfileService, messageSource, cookieHelper, gameTypeResolver, responseHelper);
    }

    @Test
    public void sendInvitesViaEmailShouldReturnForbiddenWhenNoSessionIsPresent() throws IOException {
        reset(lobbySessionCache);

        underTest.sendInvitesViaEmail(new String[]{"address1"}, "aSource", false, request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void sendAsyncInvitesViaEmailShouldReturnForbiddenWhenNoSessionIsPresent() throws IOException {
        reset(lobbySessionCache);

        underTest.sendAsyncInvitesViaEmail(new String[]{"address1"}, "aSource", request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void trackFacebookInvitesSentShouldReturnForbiddenWhenNoSessionIsPresent() throws IOException {
        reset(lobbySessionCache);

        underTest.trackFacebookInvitesSent(request, response, "aSource", "requestIds");

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getFacebookShouldAddGoogleAsProvider() {
        final ModelAndView facebookInviteFriends = underTest.getFacebookInviteFriends(request, response);

        Assert.assertThat((List<ResponseHelper.ProviderVO>) facebookInviteFriends.getModelMap().get("providers"), hasItem(gmailProvider));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getEmailShouldAddGoogleAsProvider() {
        final ModelAndView emailInviteFriends = underTest.getEmailInvitationPage(request, response);
        Assert.assertThat((List<ResponseHelper.ProviderVO>) emailInviteFriends.getModelMap().get("providers"), hasItem(gmailProvider));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getGoogleShouldAddGoogleAsProvider() {
        final ModelAndView googleInviteFriends = underTest.getGoogleInviteFriends(request, response);
        final ResponseHelper.ProviderVO provider = new ResponseHelper.ProviderVO("gmail", "gmail", "GMail Contacts", true, true);
        Assert.assertThat((List<ResponseHelper.ProviderVO>) googleInviteFriends.getModelMap().get("providers"), hasItem(provider));
    }

    @Test
    public void shouldRedirectFromStandardView() {
        RedirectView view = (RedirectView) underTest.getInvitationRootPage();
        assertEquals(view.getUrl(), "/invitation/email");
    }

    @Test
    public void shouldReturnOffCanvasEmailView() {
        ModelAndView modelAndView = underTest.getEmailInvitationPage(request, response);
        assertClassesMatch(modelAndView, "email", "start", "invitation");
        assertFalse(modelAndView.getModelMap().containsKey("showPersonSelector"));
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertEquals(PRE_SEND_HEADER, modelAndView.getModelMap().get("pageHeaderType"));
        assertEquals(PAGE_TYPE, modelAndView.getModelMap().get("pageType"));
        assertEquals(SEND_INVITE_CTA_TEXT, modelAndView.getModelMap().get("sendCtaText"));
    }

    @Test
    public void shouldRedirectToFacebookWhenEmailViewRequestedOnCanvas() {
        setOnCanvas(true);
        ModelAndView modelAndView = underTest.getEmailInvitationPage(request, response);
        RedirectView view = (RedirectView) modelAndView.getView();
        assertEquals(view.getUrl(), "./facebook");
    }

    @Test
    public void shouldReturnStandardSentPage() {
        ModelAndView modelAndView = underTest.getInvitationSentPage(request, response);
        assertClassesMatch(modelAndView, "email", "sent", "invitation");
        assertFalse(modelAndView.getModelMap().containsKey("showPersonSelector"));
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertEquals(POST_SEND_HEADER, modelAndView.getModelMap().get("pageHeaderType"));
        assertEquals(PAGE_TYPE, modelAndView.getModelMap().get("pageType"));
    }

    @Test
    public void shouldReturnPersonSelectorSentPageOnCanvas() {
        setOnCanvas(true);
        ModelAndView modelAndView = underTest.getInvitationSentPage(request, response);
        assertClassesMatch(modelAndView, "facebook", "sent", "invitation");
        assertTrue(modelAndView.getModelMap().containsKey("showPersonSelector"));
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertEquals(POST_SEND_HEADER, modelAndView.getModelMap().get("pageHeaderType"));
        assertEquals(PAGE_TYPE, modelAndView.getModelMap().get("pageType"));
    }

    @Test
    public void shouldReturnFacebookView() {
        ModelAndView modelAndView = underTest.getFacebookInviteFriends(request, response);
        assertClassesMatch(modelAndView, "facebook", "start", "invitation");
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertEquals(true, modelAndView.getModelMap().get("showPersonSelector"));
        assertEquals(PRE_SEND_HEADER, modelAndView.getModelMap().get("pageHeaderType"));
        assertEquals(PAGE_TYPE, modelAndView.getModelMap().get("pageType"));
    }

    @Test
    public void shouldDelegateSendingInvitesToInvitationLobbyService() throws IOException {
        final InvitationSendingResult expected = new InvitationSendingResult(2, new HashSet<InvitationSendingResult.Rejection>());
        when(invitationLobbyService.sendInvitations(PLAYER_ID,
                APP_INFO,
                SOURCE,
                "",
                new String[]{"a@b.com", "c@d.com"},
                true,
                USER_IP_ADDRESS)).thenReturn(expected);
        final InvitationSendingResult actual = underTest.sendInvitesViaEmail(new String[]{"a@b.com", "c@d.com"},
                SOURCE,
                true,
                request,
                response);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldAcceptSpacesBetweenEmailAddress() throws IOException {
        final InvitationSendingResult expected = new InvitationSendingResult(2, new HashSet<InvitationSendingResult.Rejection>());
        when(invitationLobbyService.sendInvitations(PLAYER_ID,
                APP_INFO,
                SOURCE,
                "",
                new String[]{"a@b.com", "c@d.com"},
                true,
                USER_IP_ADDRESS)).thenReturn(expected);
        final InvitationSendingResult actual = underTest.sendInvitesViaEmail(new String[]{"a@b.com", "c@d.com"},
                SOURCE,
                true,
                request,
                response);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldTrackingSendingEmailInvitesIfAtLeastOneIsSent() {
        final InvitationSendingResult expected = new InvitationSendingResult(2, new HashSet<InvitationSendingResult.Rejection>());
        when(invitationLobbyService.sendInvitations(PLAYER_ID,
                APP_INFO,
                SOURCE,
                "",
                new String[]{"a@b.com", "c@d.com"},
                true,
                USER_IP_ADDRESS)).thenReturn(expected);
        underTest.sendInvitesViaEmail(new String[]{"a@b.com", "c@d.com"}, SOURCE, true, request, response);
        verify(tracking).trackSuccessfulInviteFriends(PLAYER_ID, EMAIL, 2);
    }

    @Test
    public void sendAsyncInvitesShouldTrack() {
        when(invitationLobbyService.sendInvitationsAsync(PLAYER_ID,
                APP_INFO,
                SOURCE,
                "",
                new String[]{"a@b.com", "c@d.com"},
                USER_IP_ADDRESS)).thenReturn(true);
        underTest.sendAsyncInvitesViaEmail(new String[]{"a@b.com", "c@d.com"}, SOURCE, request, response);
        verify(tracking).trackSuccessfulInviteFriends(PLAYER_ID, EMAIL, 2);
    }

    @Test
    public void failedInviteShouldNotTrackInvites() {
        when(invitationLobbyService.sendInvitationsAsync(PLAYER_ID,
                APP_INFO,
                SOURCE,
                "",
                new String[]{"a@b.com", "c@d.com"},
                USER_IP_ADDRESS)).thenReturn(false);
        underTest.sendAsyncInvitesViaEmail(new String[]{"a@b.com", "c@d.com"}, SOURCE, request, response);
        verifyNoMoreInteractions(tracking);
    }

    @Test
    public void shouldNotTrackingSendingEmailInvitesIfNoneSent() {
        final InvitationSendingResult expected = new InvitationSendingResult(0, new HashSet<InvitationSendingResult.Rejection>());
        when(invitationLobbyService.sendInvitations(PLAYER_ID,
                APP_INFO,
                SOURCE,
                "",
                new String[]{"a@b.com", "c@d.com"},
                true,
                USER_IP_ADDRESS)).thenReturn(expected);
        underTest.sendInvitesViaEmail(new String[]{"a@b.com", "c@d.com"}, SOURCE, true, request, response);
        verifyZeroInteractions(tracking);
    }

    @Test
    public void shouldCheckRegisteredEmail() {
        final Set<String> expected = Collections.singleton("email2@example.org");
        when(playerProfileService.findByEmailAddresses("email1@example.org", "exmaple2@example.org"))
                .thenReturn(Collections.singletonMap("email2@example.org", BigDecimal.ZERO));
        final Set<String> actual = underTest.checkRegisteredEmails("email1@example.org,exmaple2@example.org");
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCheckRegisteredEmailWithSpaces() {
        final Set<String> expected = Collections.singleton("email1@example.org");
        when(playerProfileService.findByEmailAddresses("email1@example.org", "exmaple2@example.org"))
                .thenReturn(Collections.singletonMap("email1@example.org", BigDecimal.ZERO));
        final Set<String> actual = underTest.checkRegisteredEmails("email1@example.org , exmaple2@example.org");
        assertEquals(expected, actual);
    }

    @Test
    public void shouldNotCheckRegisteredEmailsIfEmailsAreEmpty() {
        assertEquals(Collections.<String>emptySet(), underTest.checkRegisteredEmails(""));
        assertEquals(Collections.<String>emptySet(), underTest.checkRegisteredEmails(" "));
        verifyZeroInteractions(playerProfileService);
    }

    @Test
    public void shouldNotCheckRegisteredEmailsIfEmailsAreNull() {
        assertEquals(Collections.<String>emptySet(), underTest.checkRegisteredEmails(null));
        verifyZeroInteractions(playerProfileService);
    }

    @Test
    public void shouldCheckRegisteredFacebookIds() {
        final Set<String> expected = Collections.singleton("fb1");
        when(playerProfileService.findByProviderNameAndExternalIds("FACEBOOK", "fb1", "fb2")).thenReturn(Collections.singletonMap("fb1", BigDecimal.ZERO));
        final Set<String> actual = underTest.checkRegisteredFacebookUsers("fb1,fb2");
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCheckRegisteredFacebookIdsWithSpaces() {
        final Set<String> expected = Collections.singleton("fb2");
        when(playerProfileService.findByProviderNameAndExternalIds("FACEBOOK", "fb1", "fb2")).thenReturn(Collections.singletonMap("fb2", BigDecimal.ZERO));
        final Set<String> actual = underTest.checkRegisteredFacebookUsers(" fb1  , fb2");
        assertEquals(expected, actual);
    }

    @Test
    public void shouldNotCheckRegisteredFacebookIdsIfEmpty() {
        assertEquals(Collections.<String>emptySet(), underTest.checkRegisteredFacebookUsers(""));
        assertEquals(Collections.<String>emptySet(), underTest.checkRegisteredFacebookUsers(" "));
        verifyZeroInteractions(playerProfileService);
    }

    @Test
    public void shouldNotCheckRegisteredFacebookIdsIfNull() {
        assertEquals(Collections.<String>emptySet(), underTest.checkRegisteredFacebookUsers(null));
        verifyZeroInteractions(playerProfileService);
    }

    @Test
    public void getInvitationPageShouldPopulateModelWithGameTypeSpecificText() {
        when(messageSource.getMessage("invite.gameType.text", null, null)).thenReturn("meh");

        final ModelAndView invitationPage = underTest.getFacebookInviteFriends(request, response);

        assertThat((String) invitationPage.getModel().get("gameSpecificInviteText"), is(equalTo("meh")));
    }

    @Test
    public void getInvitationPageShouldPopulateModelWithDefaultSpecificTextIfGameTypeIsMissing() {
        when(messageSource.getMessage("invite.gameType.text", null, null)).thenReturn("");
        when(messageSource.getMessage("invite.default.text", null, null)).thenReturn("meh");

        final ModelAndView invitationPage = underTest.getFacebookInviteFriends(request, response);
        assertThat((String) invitationPage.getModel().get("gameSpecificInviteText"), is(equalTo("meh")));
    }

    @Test
    public void shouldTrackFacebookInvitesSent() {
        underTest.trackFacebookInvitesSent(request, response, SOURCE, "a,b,c");
        verify(invitationLobbyService).trackFacebookInvites(PLAYER_ID, GAME_TYPE, SOURCE, "a", "b", "c");
    }

    @Test
    public void getInvitationTextShouldReturnHardCodedTitle() throws IOException {
        ArgumentCaptor<InvitationController.InvitationText> captor = ArgumentCaptor.forClass(InvitationController.InvitationText.class);

        underTest.getInvitationText(response, GAME_TYPE);

        verify(responseHelper).writeOk(same(response), captor.capture());
        assertThat(captor.getValue().getTitle(), equalTo("Invite your friends to Yazino, get 5,000 chips!"));
    }

    @Test
    public void getInvitationTextShouldReturnGameSpecificText() throws IOException {
        when(messageSource.getMessage("invite.gameType.text", null, null)).thenReturn("meh");
        ArgumentCaptor<InvitationController.InvitationText> captor = ArgumentCaptor.forClass(InvitationController.InvitationText.class);

        underTest.getInvitationText(response, GAME_TYPE);

        verify(responseHelper).writeOk(same(response), captor.capture());
        assertThat(captor.getValue().getMessage(), equalTo("meh"));
    }

    @Test
    public void getInvitationTextShouldReturnGameSpecificTextIfGameTypeIsMissing() throws IOException {
        when(messageSource.getMessage("invite.gameType.text", null, null)).thenReturn("");
        when(messageSource.getMessage("invite.default.text", null, null)).thenReturn("meh");
        ArgumentCaptor<InvitationController.InvitationText> captor = ArgumentCaptor.forClass(InvitationController.InvitationText.class);

        underTest.getInvitationText(response, GAME_TYPE);

        verify(responseHelper).writeOk(same(response), captor.capture());
        assertThat(captor.getValue().getMessage(), equalTo("meh"));
    }

    private void setOnCanvas(final boolean b) {
        when(cookieHelper.isOnCanvas(request, response)).thenReturn(b);
    }

}
