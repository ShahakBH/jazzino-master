package com.yazino.web.controller.social;

import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.service.InvitationLobbyService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChallengeControllerTest extends AbstractSocialFlowTest {
    private static String PRE_SEND_HEADER = "preSend";
    private static String PAGE_TYPE = "challenge";
    private static final String HI_STAKES = "HI_STAKES";
    private static final String SEND_CHALLENGE_CTA_TEXT = "Send Challenge";

    @Mock
    private InvitationLobbyService invitationLobbyService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private GameTypeResolver gameTypeResolver;
    @Mock
    private LobbySession session;

    private ChallengeController underTest;

    @Before
    public void setUp() {
        final CookieHelper cookieHelper = mock(CookieHelper.class);

        underTest = new ChallengeController(lobbySessionCache, invitationLobbyService, gameTypeResolver, cookieHelper);
    }

    @Test
    public void shouldRedirectFromStandardView() {
        RedirectView view = (RedirectView) underTest.getChallengeRootPage(request, response);
        assertEquals(view.getUrl(), "/challenge/buddies");
    }

    @Test
    public void shouldReturnEmailViewWithPresentMessage() {
        ModelAndView modelAndView = underTest.getEmailChallengePage(request, response);
        ModelMap model = modelAndView.getModelMap();
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertClassesMatch(model, "email", "start", "challenge");
        assertFalse(model.containsKey("showPersonSelector"));
        assertEquals(PRE_SEND_HEADER, model.get("pageHeaderType"));
        assertEquals(PAGE_TYPE, model.get("pageType"));
        assertEquals(SEND_CHALLENGE_CTA_TEXT, model.get("sendCtaText"));
        assertEquals("/challenge", model.get("sendEndpoint"));
    }

    //buddies
    //pre sent
    @Test
    public void shouldReturnBuddiesPage() {
        ModelAndView modelAndView = underTest.getBuddiesChallengePage(request, response);
        ModelMap model = modelAndView.getModelMap();
        assertClassesMatch(model, "buddies", "start", "challenge");
        assertEquals(true, model.get("showPersonSelector"));
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertEquals(PRE_SEND_HEADER, model.get("pageHeaderType"));
        assertEquals(PAGE_TYPE, model.get("pageType"));
        assertEquals(SEND_CHALLENGE_CTA_TEXT, model.get("sendCtaText"));
    }


    @Test
    public void shouldReturnFacebookView() {
        ModelAndView modelAndView = underTest.getFacebookChallengePage(request, response);
        ModelMap model = modelAndView.getModelMap();
        assertClassesMatch(model, "facebook", "start", "challenge");
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertEquals(true, model.get("showPersonSelector"));
        assertEquals(PRE_SEND_HEADER, model.get("pageHeaderType"));
        assertEquals(PAGE_TYPE, model.get("pageType"));
        assertEquals(SEND_CHALLENGE_CTA_TEXT, model.get("sendCtaText"));
    }

    @Test
    public void shouldReturnGmailView() {
        ModelAndView modelAndView = underTest.getGmailChallengePage(request, response);
        ModelMap model = modelAndView.getModelMap();
        assertClassesMatch(model, "gmail", "start", "challenge");
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertEquals(true, model.get("showPersonSelector"));
        assertEquals(PRE_SEND_HEADER, model.get("pageHeaderType"));
        assertEquals(PAGE_TYPE, model.get("pageType"));
        assertEquals(SEND_CHALLENGE_CTA_TEXT, model.get("sendCtaText"));
    }

    //sent
    @Test
    public void shouldReturnBuddyViewWithSentMessageAfterEndOfFlow() {
        ModelAndView modelAndView = underTest.getChallengeSentPage(request, response);
        ModelMap model = modelAndView.getModelMap();
        assertClassesMatch(model, "buddies", "challenge", "sent");
        assertTrue(modelAndView.getModelMap().containsKey("showPersonSelector"));
        assertEquals("partials/social-flow/layout", modelAndView.getViewName());
        assertEquals("postSend", model.get("pageHeaderType"));
        assertEquals(SEND_CHALLENGE_CTA_TEXT, model.get("sendCtaText"));
        assertEquals("/challenge", model.get("sendEndpoint"));
    }

    @Test
    public void sendEmailsShouldSendEmailsViaService() {
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(HI_STAKES);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        when(session.getPlayerId()).thenReturn(BigDecimal.TEN);
        final String id1 = "123";
        final String id2 = "456";
        List<BigDecimal> ids = newArrayList(new BigDecimal(id1), new BigDecimal(id2));
        List<String> emails = newArrayList("bob@bob.com", "jim@jim.com");
        underTest.sendEmails(request, response, new String[]{id1, id2}, new String[]{emails.get(0), emails.get(1)});

        verify(invitationLobbyService).challengeBuddies(BigDecimal.TEN, HI_STAKES, ids);
    }
}
