package com.yazino.web.service;

import com.google.common.collect.Sets;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.email.EmailValidationService;
import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.email.InviteFriendsEmailDetails;
import com.yazino.web.domain.email.ToFriendEmailBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class InvitationLobbyEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(InvitationLobbyEmailService.class);

    private final QuietPlayerEmailer emailer;
    private final EmailValidationService emailValidationService;
    private final PlayerProfileService playerProfileService;
    private final InvitationService invitationService;
    private final TrackingService trackingService;
    private final InvitationLimiter limiter;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public InvitationLobbyEmailService(final QuietPlayerEmailer emailer,
                                       final EmailValidationService emailValidationService,
                                       final PlayerProfileService playerProfileService,
                                       final InvitationService invitationService,
                                       final NoTrackingService trackingService,
                                       final InvitationLimiter limiter,
                                       final YazinoConfiguration yazinoConfiguration) {
        notNull(emailValidationService, "emailValidationService may not be null");

        this.emailer = emailer;
        this.emailValidationService = emailValidationService;
        this.playerProfileService = playerProfileService;
        this.invitationService = invitationService;
        this.trackingService = trackingService;
        this.limiter = limiter;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public InvitationLobbyEmailService(final QuietPlayerEmailer emailer,
                                       final EmailValidationService emailValidationService,
                                       final PlayerProfileService playerProfileService,
                                       final InvitationService invitationService,
                                       final NoTrackingService trackingService,
                                       final YazinoConfiguration yazinoConfiguration) {
        this(emailer, emailValidationService, playerProfileService, invitationService, trackingService, null, yazinoConfiguration);
    }

    public Boolean sendEmailsAsynchronouslyWithoutValidating(final BigDecimal playerId,
                                                             final String gameType,
                                                             final String source,
                                                             final String[] emailAddresses,
                                                             final InviteFriendsEmailDetails emailBuilder,
                                                             final String senderIpAddress) {
        if (!limiter.canSendInvitations(emailAddresses.length, playerId, senderIpAddress)) {
            return false;
        }

        sendEmails(playerId, gameType, source, emailBuilder, Sets.newHashSet(emailAddresses));
        limiter.hasSentInvitations(emailAddresses.length, playerId, senderIpAddress);
        return true;
    }

    public InvitationSendingResult emailFriends(final BigDecimal playerId,
                                                final String gameType,
                                                final String source,
                                                final String[] emailAddresses,
                                                final InviteFriendsEmailDetails emailBuilder,
                                                final boolean requireAllValidToSend,
                                                final String senderIpAddress) {

        if (!limiter.canSendInvitations(emailAddresses.length, playerId, senderIpAddress)) {
            final Set<InvitationSendingResult.Rejection> rejections = new HashSet<>();
            for (final String emailAddress : emailAddresses) {
                rejections.add(new InvitationSendingResult.Rejection(emailAddress, InvitationSendingResult.ResultCode.LIMIT_EXCEEDED));
            }
            return new InvitationSendingResult(0, rejections);
        }
        if (yazinoConfiguration.getBoolean("strata.invitation.invalidateAll", false)) {
            final Set<InvitationSendingResult.Rejection> rejections = new HashSet<>();
            for (final String emailAddress : emailAddresses) {
                rejections.add(new InvitationSendingResult.Rejection(emailAddress, InvitationSendingResult.ResultCode.INVALID_ADDRESS));
            }
            return new InvitationSendingResult(0, rejections);
        }
        final Set<InvitationSendingResult.Rejection> rejections = new HashSet<>();
        final List<String> validEmails = validateAddresses(emailAddresses, rejections);
        final Set<String> validUnregisteredEmails = checkEmailsAlreadyRegistered(validEmails, rejections);

        if (rejections.size() > 0 && requireAllValidToSend) {
            return new InvitationSendingResult(0, rejections);
        }

        sendEmails(playerId, gameType, source, emailBuilder, validUnregisteredEmails);
        limiter.hasSentInvitations(emailAddresses.length, playerId, senderIpAddress);

        return new InvitationSendingResult(validUnregisteredEmails.size(), rejections);
    }

    public InvitationSendingResult challengeBuddies(
            final List<String> emailAddresses,
            final ToFriendEmailBuilder emailBuilder,
            final BigDecimal playerId) {
        final Set<InvitationSendingResult.Rejection> rejections = newHashSet();
        final List<String> validEmailAddresses = validateAddresses(emailAddresses.toArray(new String[emailAddresses.size()]), rejections);
        for (final String emailAddress : validEmailAddresses) {
            emailer.quietlySendEmail(emailBuilder.withFriendEmailAddress(emailAddress).buildRequest(playerProfileService));
        }

        final HashMap<String, String> props = newHashMap();
        props.put("challengeSource", "overlay");
        props.put("challengeType", "email");
        props.put("playersChallenged", validEmailAddresses.size() + "");
        trackingService.trackEvent(null, playerId, "sentChallenges", props);
        return new InvitationSendingResult(validEmailAddresses.size(), rejections);
    }

    private void sendEmails(final BigDecimal playerId,
                            final String gameType,
                            final String source,
                            final InviteFriendsEmailDetails details,
                            final Set<String> validUnregisteredEmails) {
        final DateTime now = new DateTime();
        final String displayName = playerProfileService.findByPlayerId(playerId).getDisplayName();
        for (final String emailAddress : validUnregisteredEmails) {
            LOG.debug("Sending invitation to: {}", emailAddress);
            invitationService.sendEmailInvitation(playerId, displayName, emailAddress, details.getCustomisedMessage(),
                    details.getCallToActionUrl(), now, gameType, source);
        }
    }

    private Set<String> checkEmailsAlreadyRegistered(final List<String> validEmails, final Set<InvitationSendingResult.Rejection> rejections) {
        final Set<String> validUnregisteredEmails = new HashSet<>(validEmails);
        final String[] emailAddresses = validEmails.toArray(new String[validEmails.size()]);
        final Map<String, BigDecimal> registeredEmailAddresses = playerProfileService.findByEmailAddresses(emailAddresses);
        validUnregisteredEmails.removeAll(registeredEmailAddresses.keySet());
        for (String emailAddress : registeredEmailAddresses.keySet()) {
            LOG.debug("Address already registered: {}", emailAddress);
            rejections.add(new InvitationSendingResult.Rejection(emailAddress, InvitationSendingResult.ResultCode.ALREADY_REGISTERED));
        }
        return validUnregisteredEmails;
    }

    private List<String> validateAddresses(final String[] emailAddresses, final Set<InvitationSendingResult.Rejection> rejections) {
        final List<String> validEmails = new ArrayList<>();
        for (String emailAddress : emailAddresses) {
            if (emailAddress == null) {
                LOG.debug("Received null email address; addresses were {}", ArrayUtils.toString(emailAddresses));

            } else if (!emailValidationService.validate(emailAddress)) {
                rejections.add(new InvitationSendingResult.Rejection(emailAddress, InvitationSendingResult.ResultCode.INVALID_ADDRESS));
                LOG.debug("Address failed validation: {}", emailAddress);

            } else {
                validEmails.add(emailAddress);
            }
        }
        return validEmails;
    }

}
