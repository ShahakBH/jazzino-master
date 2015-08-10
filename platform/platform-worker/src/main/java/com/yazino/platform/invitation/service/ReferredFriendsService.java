package com.yazino.platform.invitation.service;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.invitation.emailService.AcceptedInviteFriendsEmailService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service("referredFriendService")
public class ReferredFriendsService {
    private static final Logger LOG = LoggerFactory.getLogger(ReferredFriendsService.class);

    private final AcceptedInviteFriendsEmailService acceptedInviteFriendsEmailService;
    private final PlayerService playerService;
    private final PlayerProfileService playerProfileService;
    private final PlayerCreditConfiguration playerCreditConfiguration;
    private final String fromAddress;


    @Autowired(required = true)
    public ReferredFriendsService(final AcceptedInviteFriendsEmailService acceptedInviteFriendsEmailService,
                                  final PlayerService playerService,
                                  final PlayerCreditConfiguration playerCreditConfiguration,
                                  final PlayerProfileService playerProfileService,
                                  @Value("${strata.email.from-address}") final String fromAddress) {
        notNull(playerCreditConfiguration);
        notNull(acceptedInviteFriendsEmailService);
        notNull(playerService);
        notNull(playerProfileService);
        notNull(fromAddress);

        this.fromAddress = fromAddress;
        this.playerCreditConfiguration = playerCreditConfiguration;
        this.acceptedInviteFriendsEmailService = acceptedInviteFriendsEmailService;
        this.playerService = playerService;
        this.playerProfileService = playerProfileService;

    }

    public BigDecimal processReferral(final BigDecimal receiverPlayerId, final BigDecimal senderPlayerId) {
        final PlayerProfile sender = playerProfileService.findByPlayerId(senderPlayerId);
        final PlayerProfile receiver = playerProfileService.findByPlayerId(receiverPlayerId);
        bebuddySenderAndReceiver(senderPlayerId, receiverPlayerId);
        sendInviteAcceptedEmail(sender, receiver);
        return creditSender(receiverPlayerId, senderPlayerId);
    }

    private void bebuddySenderAndReceiver(final BigDecimal senderPlayerId, final BigDecimal receiverPlayerId) {
        try {
            playerService.registerFriends(senderPlayerId, newHashSet(receiverPlayerId));
            playerService.registerFriends(receiverPlayerId, newHashSet(senderPlayerId));
        } catch (Exception e) {
            LOG.warn(String.format("befriending users %s and %s failed", senderPlayerId, receiverPlayerId), e);
        }
    }

    private BigDecimal creditSender(final BigDecimal receiverPlayerId,
                                    final BigDecimal senderPlayerId) {
        try {
            final BigDecimal referralAmount = playerCreditConfiguration.getReferralAmount();
            playerService.postTransaction(senderPlayerId, null, referralAmount,
                    "Referral", String.format("Player with id %s accepted invite", receiverPlayerId));
            return referralAmount;
        } catch (WalletServiceException e) {
            LOG.error("Failed to post chips", e);
            return BigDecimal.ZERO;
        }
    }

    private void sendInviteAcceptedEmail(final PlayerProfile sender,
                                         final PlayerProfile receiver) {
        if (isBlank(sender.getEmailAddress())) {
            LOG.debug("Invite accepted email will not be sent as player {} has no email address",
                    sender.getPlayerId());
            return;
        }

        try {
            acceptedInviteFriendsEmailService.sendInviteFriendsAcceptedEmail(
                    sender.getDisplayName(), sender.getEmailAddress(), receiver.getFirstName(), formattedEmailWithName(sender.getDisplayName(), fromAddress));
        } catch (Exception e) {
            LOG.error("Failed to send email referral", e);
        }
    }

    private String formattedEmailWithName(final String displayName, final String sender) {
        if (sender.indexOf("<") > 0) {
            final String email = sender.split("<")[1];
            final int end;
            if (email.lastIndexOf(">") > 0) {
                end = email.lastIndexOf(">");
            } else {
                end = email.length();
            }
            return String.format("%s <%s>", displayName, email.substring(0, end));

        }
        return String.format("%s <%s>", displayName, sender);
    }
}
