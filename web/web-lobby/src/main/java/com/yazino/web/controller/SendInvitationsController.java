package com.yazino.web.controller;

import com.yazino.platform.invitation.InvitationService;
import com.yazino.web.domain.ApplicationInformation;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.InvitationLobbyService;
import com.yazino.web.service.InvitationSendingResult;
import com.yazino.web.service.InviteFriendsTracking;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class SendInvitationsController {
    private static final Logger LOG = LoggerFactory.getLogger(SendInvitationsController.class);

    static final String SEND_INVITATIONS = "sendInvitations";

    private final SiteConfiguration siteConfiguration;
    private final LobbySessionCache lobbySessionCache;
    private final InviteFriendsTracking inviteFriendsTracking;
    private final InvitationLobbyService invitationLobbyService;

    private CookieHelper cookieHelper = new CookieHelper();

    @Autowired(required = true)
    public SendInvitationsController(@Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                                     @Qualifier("siteConfiguration") final SiteConfiguration siteConfiguration,
                                     final InvitationService invitationService,
                                     final InviteFriendsTracking inviteFriendsTracking,
                                     final InvitationLobbyService invitationLobbyService) {

        notNull(invitationService, "invitationService may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.siteConfiguration = siteConfiguration;
        this.inviteFriendsTracking = inviteFriendsTracking;
        this.invitationLobbyService = invitationLobbyService;
    }

    @RequestMapping(value = {"/lobby/inviteFriends/YAZINO","/lobby/inviteFriends/PLAY_FOR_FUN", "/friends/sendInvitation"},
            method = RequestMethod.GET)
    public String viewSendInvitations() {
        return SEND_INVITATIONS;
    }

    @RequestMapping(value = {"/lobby/inviteFriends/YAZINO","/lobby/inviteFriends/PLAY_FOR_FUN", "/friends/sendInvitation"},
            method = RequestMethod.POST)
    @ResponseBody
    public InvitationSendingResult emailInvitations(
            @RequestParam(value = "sendTo") final String sendToEmailAddresses,
            @RequestParam(value = "source") final String source,
            @RequestParam(value = "message") final String message,
            final HttpServletRequest request) throws IOException {
        LOG.debug("emailInvitations - sendTo = {}, source = {}, message = {}", sendToEmailAddresses, source, message);

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            throw new IllegalStateException("No session found");
        }

        final BigDecimal playerId = lobbySession.getPlayerId();

        final InvitationSendingResult result = invitationLobbyService.sendInvitations(
                playerId, appInforFor(request, lobbySession), source, message,
                splitAndTrim(sendToEmailAddresses), false, request.getRemoteAddr());

        inviteFriendsTracking.trackSuccessfulInviteFriends(
                playerId, InviteFriendsTracking.InvitationType.EMAIL, result.getSuccessful());

        return result;
    }

    private ApplicationInformation appInforFor(final HttpServletRequest request,
                                               final LobbySession lobbySession) {
        final String gameType = cookieHelper.getLastGameType(request.getCookies(), siteConfiguration.getDefaultGameType());
        return new ApplicationInformation(gameType, lobbySession.getPartnerId(), lobbySession.getPlatform());
    }

    private String[] splitAndTrim(final String sendToEmailAddresses) {
        final String[] addresses = sendToEmailAddresses.split(",");
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = addresses[i].trim();
        }
        return addresses;
    }

    final void setCookieHelper(final CookieHelper cookieHelper) {
        this.cookieHelper = cookieHelper;
    }

    @RequestMapping(value = {"/friends/sendInvitationReminder"}, method = RequestMethod.POST)
    public void emailInvitationReminder(@RequestParam(value = "recipientId") final String recipientId,
                                        final HttpServletRequest request,
                                        final HttpServletResponse response) throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            throw new IllegalStateException("No session found");
        }

        final String gameType = cookieHelper.getLastGameType(request.getCookies(),
                siteConfiguration.getDefaultGameType());
        final BigDecimal playerId = lobbySession.getPlayerId();
        invitationLobbyService.sendInvitationReminders(playerId, new String[]{recipientId}, gameType, request.getRemoteAddr());

        response.setContentType(MediaType.APPLICATION_JSON.getType());
        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        response.getWriter().write("{}");

    }
}
