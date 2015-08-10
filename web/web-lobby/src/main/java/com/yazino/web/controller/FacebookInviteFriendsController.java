package com.yazino.web.controller;

import com.yazino.platform.community.PlayerService;
import com.yazino.platform.community.Relationship;
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
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class FacebookInviteFriendsController {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookInviteFriendsController.class);

    private final LobbySessionCache lobbySessionCache;
    private final CookieHelper cookieHelper;
    private final SiteConfiguration siteConfiguration;
    private InvitationService invitationService;
    private PlayerService playerService;
    private PlayerProfileService playerProfileService;
    private GameTypeRepository gameTypeRepository;
    private final HierarchicalMessageSource resourceBundleMessageSource;
    private final InviteFriendsTracking inviteFriendsTracking;

    @Autowired(required = true)
    public FacebookInviteFriendsController(@Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                                           @Qualifier("invitationService") final InvitationService invitationService,
                                           final PlayerService playerService,
                                           final PlayerProfileService playerProfileService,
                                           final GameTypeRepository gameTypeRepository,
                                           @Qualifier("cookieHelper") final CookieHelper cookieHelper,
                                           @Qualifier("siteConfiguration") final SiteConfiguration siteConfiguration,
                                           final HierarchicalMessageSource resourceBundleMessageSource,
                                           final InviteFriendsTracking inviteFriendsTracking) {
        notNull(gameTypeRepository, "gameTypeRepository may not be null");
        notNull(playerProfileService, "playerProfileService may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.invitationService = invitationService;
        this.gameTypeRepository = gameTypeRepository;
        this.cookieHelper = cookieHelper;
        this.siteConfiguration = siteConfiguration;
        this.playerService = playerService;
        this.playerProfileService = playerProfileService;
        this.resourceBundleMessageSource = resourceBundleMessageSource;
        this.inviteFriendsTracking = inviteFriendsTracking;
    }

    @RequestMapping({"/lobby/inviteFriends/FACEBOOK", "/lobby/inviteFacebookFriends", "/friends/inviteFromFacebook"})
    public String inviteFriends(final ModelMap model, final HttpServletRequest request) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        notNull(lobbySession, "No session found");

        model.addAttribute("referringPlayerId", lobbySession.getPlayerId());
        final String[] toExclude = getIdsToExcludeFromInvitations(lobbySession.getPlayerId());
        model.addAttribute("excludedFriendIds", StringUtils.join(toExclude, ","));
        model.addAttribute("source", "FACEBOOK_FORM");

        return "facebookInviteFriend";
    }

    @RequestMapping({"/lobby/inviteFriendsToPrivateTable/FACEBOOK", "/friends/inviteToPrivateTableFromFacebook"})
    public String inviteFriendsToPrivateTable(
            final HttpServletRequest request, final HttpServletResponse response,
            final ModelMap model,
            @RequestParam(value = "gameType", required = false) final String gameType,
            @RequestParam(value = "tableId", required = false) final String tableIdAsString,
            @RequestParam(value = "tableName", required = false) final String tableName) {
        if (!hasParameter("gameType", gameType, request, response)
                || !hasParameter("tableId", tableIdAsString, request, response)
                || !hasParameter("tableName", tableName, request, response)) {
            return null;
        }

        final BigDecimal tableId;
        try {
            tableId = new BigDecimal(tableIdAsString);
        } catch (Exception e) {
            LOG.warn("Invalid table ID provided: {}", tableIdAsString);
            return sendBadRequestStatus(response);
        }

        model.addAttribute("privateTableGameName", getGameName(gameType));
        model.addAttribute("privateTableId", tableId);
        model.addAttribute("privateTableName", tableName);

        return "privateTableFacebookFriendsInvite";
    }

    private String sendBadRequestStatus(final HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException e1) {
            // ignored
        }
        return null;
    }

    @RequestMapping({"/invitationsToPrivateTableSent/FACEBOOK",
            "/lobby/invitationsToPrivateTableSent/FACEBOOK",
            "/friends/invitedToPrivateTableFromFacebook"})
    public String invitationsToPrivateTableSent() {
        return "privateTableFacebookFriendsInviteSent";
    }

    @RequestMapping({"/inviteFriendsViaFacebook", "/lobby/inviteFriendsViaFacebook", "/friends/inviteViaFacebook"})
    public String inviteFriendsViaFacebook(
            final ModelMap model,
            final HttpServletRequest request,
            @RequestParam(value = "source", required = false) final String source,
            @RequestParam(value = "targetFriendId", required = false) final String targetFriendId) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        notNull(lobbySession, "No session found");

        model.addAttribute("referringPlayerId", lobbySession.getPlayerId());
        final String[] toExclude = getIdsToExcludeFromInvitations(lobbySession.getPlayerId());
        model.addAttribute("excludedFriendIds", StringUtils.join(toExclude, ","));
        model.addAttribute("source", source);
        LOG.info("TARGET FRIEND ID = " + targetFriendId);
        model.addAttribute("targetFriendId", targetFriendId);

        model.addAttribute("inviteMessage", getInvitationTextForGameType(source));

        return "partials/inviteFriendsViaFacebook";
    }

    private String getGameName(final String gameTypeId) {
        for (final GameTypeInformation gameTypeInformation : gameTypeRepository.getGameTypes().values()) {
            if (gameTypeInformation.getGameType().getId().equals(gameTypeId)) {
                return gameTypeInformation.getGameType().getName();
            }
        }
        return gameTypeId;
    }

    @RequestMapping({"/lobby/acknowledgeFriendsInvitedViaFacebook", "/acknowledgeFriendsInvitedViaFacebook",
            "/friends/acknowledgeInvitedViaFacebook"})
    public String friendsInvitedViaFacebook(final HttpServletRequest request,
                                            @RequestParam(value = "source", required = false) final String source) {
        try {
            final BigDecimal issuingPlayerId = findIssuingPlayerId(request);
            final String[] recipientIdentifiers = findRecipientIdentifiers(request);
            final String currentGame =
                    cookieHelper.getLastGameType(request.getCookies(), siteConfiguration.getDefaultGameType());
            final String nonNullSource;
            if (source == null || "".equals(source.trim()) || "null".equals(source) || "undefined".equals(source)) {
                final String sourceFromCookie = cookieHelper.getScreenSource(request.getCookies());
                if (sourceFromCookie == null) {
                    nonNullSource = "FACEBOOK_POPUP";
                } else {
                    nonNullSource = sourceFromCookie;
                }
            } else {
                nonNullSource = source;
            }
            notifyInvitationServiceThatInvitationsSentToFriends(issuingPlayerId, recipientIdentifiers, currentGame,
                    nonNullSource);

            if (recipientIdentifiers.length > 0) {
                inviteFriendsTracking.trackSuccessfulInviteFriends(issuingPlayerId,
                        InviteFriendsTracking.InvitationType.FACEBOOK, recipientIdentifiers.length);
            }
        } catch (final Exception e) {
            LOG.warn("An exception occurred while recording invitations sent via Facebook.", e);
        }

        return "partials/closeInviteFriendsDialog";
    }

    private BigDecimal findIssuingPlayerId(final HttpServletRequest request) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        return lobbySession.getPlayerId();
    }

    private String[] findRecipientIdentifiers(final HttpServletRequest request) {
        final String requestIds = request.getParameter("request_ids");
        if (requestIds != null) {
            return requestIds.split(",");
        }
        return new String[0];
    }

    private void notifyInvitationServiceThatInvitationsSentToFriends(final BigDecimal issuingPlayerId,
                                                                     final String[] recipientIdentifiers,
                                                                     final String currentGame,
                                                                     final String screenSource) {
        final boolean hasRecipients = recipientIdentifiers != null && recipientIdentifiers.length > 0;
        if (hasRecipients) {
            for (final String recipientIdentifier : recipientIdentifiers) {
                invitationService.invitationSent(issuingPlayerId, recipientIdentifier, InvitationSource.FACEBOOK,
                        new DateTime(), currentGame, screenSource);
            }

            LOG.debug("Invitations sent via Facebook (issuer id = " + issuingPlayerId + ", recipient identifiers = ["
                    + StringUtils.join(recipientIdentifiers, ", ") + "])");
        } else {
            LOG.warn("Invitation sent with 0 recipients (issuer id is " + issuingPlayerId + ").");
        }
    }

    private String[] getIdsToExcludeFromInvitations(final BigDecimal playerId) {
        LOG.debug("getIdsToExcludeFromInvitations {}", playerId);
        final List<String> relatedExternalIds = new ArrayList<String>();
        final Map<BigDecimal, Relationship> relationships = playerService.getRelationships(playerId);
        if (relationships != null) {
            for (final BigDecimal relatedPlayerId : relationships.keySet()) {
                try {
                    relatedExternalIds.add(playerProfileService.findByPlayerId(relatedPlayerId).getExternalId());

                } catch (final Exception e) {
                    LOG.warn("Couldn't get external ID for player: {}", relatedPlayerId);
                }
            }
        }
        final String[] arr = new String[relatedExternalIds.size()];
        for (int i = 0; i < relatedExternalIds.size(); i++) {
            arr[i] = relatedExternalIds.get(i);
        }
        return arr;
    }

    private String getInvitationTextForGameType(final String gameType) {
        if (gameType != null) {
            final String cleanedGameType = gameType.split("_IN_GAME")[0];
            try {
                final String invitationText = resourceBundleMessageSource.getMessage(
                        "invite." + cleanedGameType + ".text", null, null);

                if (StringUtils.isNotBlank(invitationText)) {
                    return invitationText;
                }
            } catch (NoSuchMessageException e) {
                LOG.warn("couldn't find message text for invite.{}.text", cleanedGameType, e);
            }
        }

        return resourceBundleMessageSource.getMessage("invite.default.text", null, null);

    }
}
