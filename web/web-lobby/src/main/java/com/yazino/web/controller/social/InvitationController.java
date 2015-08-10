package com.yazino.web.controller.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.restfb.util.StringUtils;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.service.InvitationLobbyService;
import com.yazino.web.service.InvitationSendingResult;
import com.yazino.web.service.InviteFriendsTracking;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static com.yazino.web.service.InviteFriendsTracking.InvitationType.EMAIL;


@Controller
@RequestMapping("/invitation")
public class InvitationController {
    private static final Logger LOG = LoggerFactory.getLogger(InvitationController.class);

    private final LobbySessionCache lobbySessionCache;
    private final InvitationLobbyService invitationLobbyService;
    private final InviteFriendsTracking inviteFriendsTracking;
    private final PlayerProfileService playerProfileService;
    private final HierarchicalMessageSource resourceBundleMessageSource;
    private final CookieHelper cookieHelper;
    private final GameTypeResolver gameTypeResolver;
    private final WebApiResponses responseHelper;

    private static ResponseHelper.Provider[] supportedProvidersOnCanvas;
    private static ResponseHelper.Provider[] supportedProvidersOffCanvas;

    static {
        supportedProvidersOnCanvas = new ResponseHelper.Provider[1];
        supportedProvidersOnCanvas[0] = ResponseHelper.Provider.FACEBOOK;

        supportedProvidersOffCanvas = new ResponseHelper.Provider[2];
        supportedProvidersOffCanvas[0] = ResponseHelper.Provider.GMAIL;
        supportedProvidersOffCanvas[1] = ResponseHelper.Provider.EMAIL_ADDRESS;
    }

    @Autowired
    public InvitationController(final LobbySessionCache lobbySessionCache,
                                final InvitationLobbyService invitationLobbyService,
                                final InviteFriendsTracking inviteFriendsTracking,
                                final PlayerProfileService playerProfileService,
                                final HierarchicalMessageSource resourceBundleMessageSource,
                                final CookieHelper cookieHelper,
                                final GameTypeResolver gameTypeResolver,
                                final WebApiResponses responseHelper) {
        this.lobbySessionCache = lobbySessionCache;
        this.invitationLobbyService = invitationLobbyService;
        this.inviteFriendsTracking = inviteFriendsTracking;
        this.playerProfileService = playerProfileService;
        this.resourceBundleMessageSource = resourceBundleMessageSource;
        this.cookieHelper = cookieHelper;
        this.gameTypeResolver = gameTypeResolver;
        this.responseHelper = responseHelper;
    }

    @RequestMapping(method = RequestMethod.GET)
    public View getInvitationRootPage() {
        return new RedirectView("/invitation/email", false, false, false);
    }

    @RequestMapping(value = "/email", method = RequestMethod.GET)
    public ModelAndView getEmailInvitationPage(HttpServletRequest request, HttpServletResponse response) {
        if (cookieHelper.isOnCanvas(request, response)) {
            return redirect("./facebook");
        }
        return setupResponse(ResponseHelper.Provider.EMAIL_ADDRESS, request, response).toModelAndView();
    }

    @RequestMapping(value = "/sent", method = RequestMethod.GET)
    public ModelAndView getInvitationSentPage(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        final ResponseHelper.Provider provider = providerFor(httpRequest, httpResponse);
        final ResponseHelper.ResponseBuilder response = setupResponse(provider, httpRequest, httpResponse).withSentVariation();
        if (provider == ResponseHelper.Provider.FACEBOOK) {
            addGameSpecificMessageToBuilder(httpRequest, httpResponse, response);
        }
        return response.toModelAndView();
    }

    private ResponseHelper.Provider providerFor(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        final ResponseHelper.Provider provider;
        if (cookieHelper.isOnCanvas(httpRequest, httpResponse)) {
            provider = ResponseHelper.Provider.FACEBOOK;
        } else {
            provider = ResponseHelper.Provider.EMAIL_ADDRESS;
        }
        return provider;
    }

    @RequestMapping(value = "/facebook", method = RequestMethod.GET)
    public ModelAndView getFacebookInviteFriends(final HttpServletRequest httpRequest,
                                                 final HttpServletResponse httpResponse) {
        final ResponseHelper.ResponseBuilder response = setupResponse(ResponseHelper.Provider.FACEBOOK, httpRequest, httpResponse);
        addGameSpecificMessageToBuilder(httpRequest, httpResponse, response);
        return response.toModelAndView();
    }

    private void addGameSpecificMessageToBuilder(final HttpServletRequest httpRequest,
                                                 final HttpServletResponse httpResponse,
                                                 final ResponseHelper.ResponseBuilder response) {
        final String lastGameType = gameTypeResolver.resolveGameType(httpRequest, httpResponse);
        response.withModelAttribute("gameSpecificInviteText", getInvitationTextForGameType(lastGameType));
    }

    @RequestMapping(value = "/gmail", method = RequestMethod.GET)
    public ModelAndView getGoogleInviteFriends(final HttpServletRequest httpRequest,
                                               final HttpServletResponse httpResponse) {
        final ResponseHelper.ResponseBuilder response = setupResponse(ResponseHelper.Provider.GMAIL, httpRequest, httpResponse);
        final String lastGameType = gameTypeResolver.resolveGameType(httpRequest, httpResponse);
        response.withModelAttribute("gameSpecificInviteText", getInvitationTextForGameType(lastGameType));
        return response.toModelAndView();
    }

    @RequestMapping(value = "/inviteViaEmail", method = RequestMethod.POST)
    @ResponseBody
    public InvitationSendingResult sendInvitesViaEmail(
            @RequestParam("emails[]") final String[] emailAddresses,
            @RequestParam("source") final String source,
            @RequestParam("requireAllValidToSend") boolean requireAllValidToSend,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendForbidden(response);
            return null;
        }

        final InvitationSendingResult result = invitationLobbyService.sendInvitations(
                lobbySession.getPlayerId(),
                gameTypeResolver.appInfoFor(request, response, lobbySession),
                source,
                "",
                emailAddresses,
                requireAllValidToSend,
                request.getRemoteAddr());
        if (result.getSuccessful() > 0) {
            inviteFriendsTracking.trackSuccessfulInviteFriends(
                    lobbySession.getPlayerId(), EMAIL, result.getSuccessful());

        }
        return result;
    }

    @RequestMapping(value = "/asyncInviteViaEmail", method = RequestMethod.POST)
    @ResponseBody
    public Boolean sendAsyncInvitesViaEmail(
            @RequestParam("emails[]") final String[] emailAddresses,
            @RequestParam("source") final String source,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendForbidden(response);
            return null;
        }

        final Boolean result = invitationLobbyService.sendInvitationsAsync(
                lobbySession.getPlayerId(),
                gameTypeResolver.appInfoFor(request, response, lobbySession),
                source,
                "",
                emailAddresses,
                request.getRemoteAddr());
        if (result) {
            inviteFriendsTracking.trackSuccessfulInviteFriends(lobbySession.getPlayerId(), EMAIL, emailAddresses.length);
        }
        return result;
    }

    @RequestMapping({  /* legacy URL: */ "/registered/facebook/", /* legacy URL: */ "/registered/facebook"})
    @ResponseBody
    public Set<String> checkRegisteredFacebookUsers(@RequestParam("ids") final String commaSeparatedIds) {
        LOG.debug("Checking if facebook ids are registered: {}", commaSeparatedIds);

        if (StringUtils.isBlank(commaSeparatedIds)) {
            return Collections.emptySet();
        }
        final String[] candidateIds = splitAndTrim(commaSeparatedIds);
        return playerProfileService.findByProviderNameAndExternalIds("FACEBOOK", candidateIds).keySet();
    }

    @RequestMapping(value = "/registered/email/")
    @ResponseBody
    public Set<String> checkRegisteredEmails(@RequestParam("addresses")
                                             final String commaSeparatedEmailAddresses) {
        LOG.debug("Checking if email are registered: {}", commaSeparatedEmailAddresses);
        if (StringUtils.isBlank(commaSeparatedEmailAddresses)) {
            return Collections.emptySet();
        }
        final String[] candidateIds = splitAndTrim(commaSeparatedEmailAddresses);
        return playerProfileService.findByEmailAddresses(candidateIds).keySet();
    }

    @RequestMapping(value = "/facebook", method = RequestMethod.POST)
    public ModelAndView trackFacebookInvitesSent(final HttpServletRequest request,
                                                 final HttpServletResponse response,
                                                 @RequestParam("source")
                                                 final String source,
                                                 @RequestParam("requestIds")
                                                 final String requestIds) {
        final BigDecimal playerId = playerIdForCurrentSession(request);
        if (playerId == null) {
            sendForbidden(response);
            return null;
        }

        invitationLobbyService.trackFacebookInvites(playerId,
                gameTypeResolver.resolveGameType(request, response),
                source,
                splitAndTrim(requestIds));
        return redirect("./sent");
    }

    @RequestMapping(value = "/text", method = RequestMethod.GET)
    public void getInvitationText(final HttpServletResponse response,
                                  @RequestParam("gameType") final String gameType) throws IOException {
        // Note: duplicates text in social/configuration.vm but not a priority to deal with this right now...
        String title = "Invite your friends to Yazino, get 5,000 chips!";
        String message = getInvitationTextForGameType(gameType);
        responseHelper.writeOk(response, new InvitationText(title, message));
    }


    private void sendForbidden(final HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private BigDecimal playerIdForCurrentSession(final HttpServletRequest request) {
        final LobbySession activeSession = lobbySessionCache.getActiveSession(request);
        if (activeSession == null) {
            return null;
        }
        return activeSession.getPlayerId();
    }

    private ModelAndView redirect(final String url) {
        return new ModelAndView(new RedirectView(url, false, false, false));
    }

    private ResponseHelper.ResponseBuilder setupResponse(final ResponseHelper.Provider currentProvider,
                                                         final HttpServletRequest request,
                                                         final HttpServletResponse response) {
        return ResponseHelper.setupResponse(supportedProviders(request, response), currentProvider)
                .withPageType("invitation")
                .withModelAttribute("sendCtaText", "Send Invite");
    }

    private ResponseHelper.Provider[] supportedProviders(final HttpServletRequest request,
                                                         final HttpServletResponse response) {
        final ResponseHelper.Provider[] supportedProviders;
        if (cookieHelper.isOnCanvas(request, response)) {
            supportedProviders = supportedProvidersOnCanvas;
        } else {
            supportedProviders = supportedProvidersOffCanvas;
        }
        return supportedProviders;
    }

    private String[] splitAndTrim(final String sendToEmailAddresses) {
        final String[] addresses = sendToEmailAddresses.split(",");
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = addresses[i].trim();
        }
        return addresses;
    }

    private String getInvitationTextForGameType(final String gameType) {
        if (gameType != null) {
            final String cleanedGameType = gameType.split("_IN_GAME")[0];
            try {
                final String invitationText = resourceBundleMessageSource.getMessage("invite." + cleanedGameType + ".text", null, null);

                if (org.apache.commons.lang3.StringUtils.isNotBlank(invitationText)) {
                    return invitationText;
                }
            } catch (NoSuchMessageException e) {
                LOG.warn("couldn't find message text for invite.{}.text", cleanedGameType, e);
            }
        }
        return resourceBundleMessageSource.getMessage("invite.default.text", null, null);
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    static class InvitationText {

        @JsonProperty
        private final String message;
        private final String title;

        InvitationText(final String title, final String message) {
            this.message = message;
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public String getTitle() {
            return title;
        }
    }
}
