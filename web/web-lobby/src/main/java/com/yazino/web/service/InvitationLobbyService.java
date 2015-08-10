package com.yazino.web.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.ApplicationInformation;
import com.yazino.web.domain.email.ChallengeBuddiesEmailBuilder;
import com.yazino.web.domain.email.InviteFriendsEmailDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class InvitationLobbyService {
    private static final Logger LOG = LoggerFactory.getLogger(InvitationLobbyService.class);

    private static final String PROPERTY_PUBLIC_URL = "strata.public.url";
    private static final String PROPERTY_REF_PARAM = "strata.referral.param";
    private static final String PROPERTY_FROM_ADDRESS = "strata.email.from-address";
    // format is invitation.email.cta.<partner>.<platform>.<gameType>
    private static final String PROPERTY_CTA_URL = "invitation.email.cta.%s.%s.%s";

    private final InvitationLobbyEmailService invitationLobbyEmailService;
    private final InvitationLobbyFacebookService invitationLobbyFacebookService;
    private final PlayerProfileService playerProfileService;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public InvitationLobbyService(final InvitationLobbyEmailService invitationLobbyEmailService,
                                  final InvitationLobbyFacebookService invitationLobbyFacebookService,
                                  final PlayerProfileService playerProfileService,
                                  final YazinoConfiguration yazinoConfiguration) {
        notNull(invitationLobbyEmailService, "invitationLobbyEmailService may not be null");
        notNull(invitationLobbyFacebookService, "invitationLobbyFacebookService may not be null");
        notNull(playerProfileService, "playerProfileService may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.invitationLobbyEmailService = invitationLobbyEmailService;
        this.invitationLobbyFacebookService = invitationLobbyFacebookService;
        this.playerProfileService = playerProfileService;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public InvitationSendingResult sendInvitations(final BigDecimal playerId,
                                                   final ApplicationInformation appInfo,
                                                   final String source,
                                                   final String message,
                                                   final String[] emailAddresses,
                                                   final boolean requireAllValidToSend,
                                                   final String senderIpAddress) {
        LOG.debug("Player {} - sending invitations to {}", playerId, emailAddresses);

        final InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(message, callToActionUrlFor(appInfo), playerId);
        return invitationLobbyEmailService.emailFriends(playerId,
                appInfo.getGameType(),
                source,
                emailAddresses,
                builder,
                requireAllValidToSend,
                senderIpAddress);
    }

    public Boolean sendInvitationsAsync(final BigDecimal playerId,
                                        final ApplicationInformation appInfo,
                                        final String source,
                                        final String message,
                                        final String[] emailAddresses,
                                        final String senderIpAddress) {
        LOG.debug("Player {} - sending invitations to {}", playerId, emailAddresses);

        final InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(message, callToActionUrlFor(appInfo), playerId);
        return invitationLobbyEmailService.sendEmailsAsynchronouslyWithoutValidating(playerId,
                appInfo.getGameType(),
                source,
                emailAddresses,
                builder,
                senderIpAddress);
    }

    private String callToActionUrlFor(final ApplicationInformation appInfo) {
        final String ctaUrlProperty = String.format(PROPERTY_CTA_URL, appInfo.getPartner(), appInfo.getPlatform(), appInfo.getGameType());
        final String ctaUrl = yazinoConfiguration.getString(ctaUrlProperty, null);
        if (ctaUrl != null) {
            return ctaUrl;
        }

        return defaultCallToActionUrlFor(appInfo.getGameType());
    }

    private String defaultCallToActionUrlFor(final String gameType) {
        final String publicUrl = yazinoConfiguration.getString(PROPERTY_PUBLIC_URL);
        final String refParam = yazinoConfiguration.getString(PROPERTY_REF_PARAM);

        if (gameType != null) {
            return publicUrl + gameType + refParam;
        }
        return publicUrl + refParam;
    }

    public InvitationSendingResult challengeBuddies(final BigDecimal playerId,
                                                    final String gameType,
                                                    final List<BigDecimal> ids) {

        final List<String> emails = Lists.transform(ids, new Function<BigDecimal, String>() {
            @Override
            public String apply(final BigDecimal id) {
                return playerProfileService.findByPlayerId(id).getEmailAddress();
            }
        });
        return challengeBuddiesWithEmails(playerId, gameType, emails);
    }

    public InvitationSendingResult challengeBuddiesWithEmails(final BigDecimal playerId,
                                                              final String gameType,
                                                              final List<String> emails) {
        final String[] referral = buildReferralUrl(
                yazinoConfiguration.getString(PROPERTY_PUBLIC_URL) + yazinoConfiguration.getString(PROPERTY_REF_PARAM), gameType);
        final ChallengeBuddiesEmailBuilder builder = new ChallengeBuddiesEmailBuilder(referral,
                playerId,
                gameType,
                yazinoConfiguration.getString(PROPERTY_FROM_ADDRESS));
        return invitationLobbyEmailService.challengeBuddies(emails, builder, playerId);
    }

    String[] buildReferralUrl(final String referralUrl, final String gameType) {
        final String[] url = referralUrl.split("/\\?ref=");

        final String refUrl = url[1].substring(0,
                url[1].lastIndexOf("_")) + "_" + gameType.toLowerCase() + "_challenge";
        return new String[]{url[0] + "/" + gameType + "?ref=", refUrl};
    }

    public void sendInvitationReminders(final BigDecimal playerId,
                                        final String[] emailAddresses,
                                        final String gameType,
                                        final String senderIpAddress) {
        LOG.debug("Player {} - sending invite reminders to {}", playerId, emailAddresses);

        final String effectiveReferralUrl = String.format(yazinoConfiguration.getString(PROPERTY_PUBLIC_URL), playerId);

        final InviteFriendsEmailDetails builder =
                new InviteFriendsEmailDetails(null, effectiveReferralUrl, playerId);

        invitationLobbyEmailService.emailFriends(playerId,
                gameType,
                "INVITATION_STATEMENT",
                emailAddresses,
                builder,
                false,
                senderIpAddress);
    }

    public void trackFacebookInvites(final BigDecimal playerId,
                                     final String gameType,
                                     final String source,
                                     final String... facebookRequestIds) {
        invitationLobbyFacebookService.trackInvitesSent(playerId, gameType, source, facebookRequestIds);
    }

}
