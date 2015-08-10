package com.yazino.web.controller.social;

import com.google.common.base.Function;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.service.InvitationLobbyService;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.yazino.web.controller.social.ResponseHelper.Provider.GMAIL;

@Controller("challengeController")
@RequestMapping("/challenge")
public class ChallengeController {

    private static ResponseHelper.Provider[] supportedOffCanvasProviders;
    private static ResponseHelper.Provider[] supportedOnCanvasProviders;
    private final InvitationLobbyService invitationLobbyService;
    private final GameTypeResolver gameTypeResolver;
    private LobbySessionCache lobbySessionCache;
    private final CookieHelper cookieHelper;

    static {
        supportedOffCanvasProviders = new ResponseHelper.Provider[3];
        supportedOffCanvasProviders[0] = ResponseHelper.Provider.BUDDIES;
        supportedOffCanvasProviders[1] = GMAIL;
        supportedOffCanvasProviders[2] = ResponseHelper.Provider.EMAIL_ADDRESS;
        supportedOnCanvasProviders = new ResponseHelper.Provider[1];
        supportedOnCanvasProviders[0] = ResponseHelper.Provider.FACEBOOK;
    }

    @Autowired
    public ChallengeController(final LobbySessionCache lobbySessionCache,
                               final InvitationLobbyService invitationLobbyService,
                               final GameTypeResolver gameTypeResolver,
                               final CookieHelper cookieHelper) {

        this.invitationLobbyService = invitationLobbyService;
        this.gameTypeResolver = gameTypeResolver;
        this.lobbySessionCache = lobbySessionCache;
        this.cookieHelper = cookieHelper;

    }

    @RequestMapping(method = RequestMethod.GET)
    public View getChallengeRootPage(HttpServletRequest request, HttpServletResponse response) {
        final String url;
        if (cookieHelper.isOnCanvas(request, response)) {
            url = "/challenge/facebook";
        } else {
            url = "/challenge/buddies";
        }
        return new RedirectView(url, false, false, false);
    }

    @RequestMapping(value = "/buddies", method = RequestMethod.GET)
    public ModelAndView getBuddiesChallengePage(HttpServletRequest request, HttpServletResponse response) {
        return setupResponse(ResponseHelper.Provider.BUDDIES, request, response).toModelAndView();
    }

    @RequestMapping(value = "/facebook", method = RequestMethod.GET)
    public ModelAndView getFacebookChallengePage(HttpServletRequest request, HttpServletResponse response) {
        return setupResponse(ResponseHelper.Provider.FACEBOOK, request, response).toModelAndView();
    }

    @RequestMapping(value = "/gmail", method = RequestMethod.GET)
    public ModelAndView getGmailChallengePage(final HttpServletRequest request, final HttpServletResponse response) {
        return setupResponse(GMAIL, request, response).toModelAndView();
    }

    @RequestMapping(value = "/email", method = RequestMethod.GET)
    public ModelAndView getEmailChallengePage(HttpServletRequest request, HttpServletResponse response) {
        return setupResponse(ResponseHelper.Provider.EMAIL_ADDRESS, request, response)
                .toModelAndView();
    }

    @RequestMapping(value = "/sent", method = RequestMethod.GET)
    public ModelAndView getChallengeSentPage(HttpServletRequest request, HttpServletResponse response) {
        return setupResponse(ResponseHelper.Provider.BUDDIES, request, response)
                .withSentVariation()
                .toModelAndView();
    }

    private ResponseHelper.ResponseBuilder setupResponse(final ResponseHelper.Provider provider, HttpServletRequest request, HttpServletResponse response) {
        final ResponseHelper.Provider[] supportedProviders;
        if (cookieHelper.isOnCanvas(request, response)) {
            supportedProviders = supportedOnCanvasProviders;
        } else {
            supportedProviders = supportedOffCanvasProviders;
        }
        return ResponseHelper.setupResponse(supportedProviders, provider)
                .withPageType("challenge")
                .withModelAttribute("sendEndpoint", "/challenge")
                .withModelAttribute("sendCtaText", "Send Challenge");
    }

    @RequestMapping(method = RequestMethod.POST)
    public View sendEmails(final HttpServletRequest request,
                           final HttpServletResponse response,
                           @RequestParam(value = "buddyIds[]", required = false) final String[] lobbyIds,
                           @RequestParam(value = "emails[]", required = false) final String[] emails
    ) {
        final BigDecimal playerId = lobbySessionCache.getActiveSession(request).getPlayerId();
        final String gameType = gameTypeResolver.resolveGameType(request, response);
        if (lobbyIds != null && lobbyIds.length > 0) {
            invitationLobbyService.challengeBuddies(playerId, gameType, convertStringArrayToBigDecimalArray(lobbyIds));
        }
        if (emails != null && emails.length > 0) {
            invitationLobbyService.challengeBuddiesWithEmails(playerId, gameType, newArrayList(emails));
        }
        return new RedirectView("/challenge/sent", false, false, false);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public void sendEmails(final HttpServletRequest request,
                           final HttpServletResponse response,
                           @RequestParam(value = "buddyId", required = true) final String buddyId) {
        final BigDecimal playerId = lobbySessionCache.getActiveSession(request).getPlayerId();
        final String gameType = gameTypeResolver.resolveGameType(request, response);

        invitationLobbyService.challengeBuddies(playerId, gameType, newArrayList(new BigDecimal(buddyId)));

    }


    private List<BigDecimal> convertStringArrayToBigDecimalArray(final String[] lobbyIds) {
        return transform(newArrayList(lobbyIds), new Function<String, BigDecimal>() {
            @Override
            public BigDecimal apply(final String id) {
                return new BigDecimal(id);
            }
        });
    }

}
