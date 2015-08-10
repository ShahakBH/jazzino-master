package com.yazino.platform.invitation.service;

import com.yazino.platform.event.message.PlayerReferrerEvent;
import com.yazino.platform.invitation.InvitationRepository;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.InvitationStatus;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;


@Service
public class InvitationTrackingService {

    private static final Logger LOG = LoggerFactory.getLogger(InvitationTrackingService.class);

    private final InvitationRepository invitationRepository;
    private final ReferredFriendsService referredFriendsService;
    private final QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService;

    @Autowired
    public InvitationTrackingService(final InvitationRepository invitationRepository,
                                     final ReferredFriendsService referredFriendsService,
                                     @Qualifier("playerReferrerEventQueuePublishingService")
                                     final QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService) {
        this.playerReferrerEventService = playerReferrerEventService;

        notNull(invitationRepository, "invitationRepository may not be null");
        notNull(referredFriendsService, "referredFriendsService may not be null");

        this.invitationRepository = invitationRepository;
        this.referredFriendsService = referredFriendsService;
    }

    public void invitationSent(final BigDecimal issuingPlayerId,
                               final String recipientIdentifier,
                               final InvitationSource source,
                               final DateTime sendTime,
                               final String gameType,
                               final String screenSource) {

        notNull(issuingPlayerId, "issuingPlayerId");
        notNull(recipientIdentifier, "recipientIdentifier");
        notNull(source, "source");
        notNull(sendTime, "sendTime");

        final com.yazino.platform.invitation.persistence.Invitation existingInvitation =
                invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(issuingPlayerId,
                        recipientIdentifier, source);

        com.yazino.platform.invitation.persistence.Invitation updatedInvitation = null;


        if (existingInvitation == null) {
            updatedInvitation = new com.yazino.platform.invitation.persistence.Invitation(issuingPlayerId,
                    recipientIdentifier, source, InvitationStatus.WAITING,
                    sendTime, gameType, screenSource);
        } else if (existingInvitation.getStatus().equals(InvitationStatus.WAITING)) {
            updatedInvitation = new com.yazino.platform.invitation.persistence.Invitation(existingInvitation
                    .getIssuingPlayerId(),
                    existingInvitation.getRecipientIdentifier(), existingInvitation.getSource(),
                    InvitationStatus.WAITING_REMINDED, existingInvitation.getRewardAmount(),
                    existingInvitation.getCreateTime(), sendTime,
                    existingInvitation.getGameType(), existingInvitation.getScreenSource());
        } else {
            LOG.warn("Invitation already accepted: " + existingInvitation);
        }

        if (updatedInvitation != null) {
            invitationRepository.save(updatedInvitation);
        }
    }

    public void invitationAccepted(final String recipientIdentifier,
                                   final InvitationSource source,
                                   final DateTime registrationTime,
                                   final BigDecimal recipientPlayerId) {

        final Collection<com.yazino.platform.invitation.persistence.Invitation> invitations = invitationRepository
                .getInvitationsSentTo(
                        recipientIdentifier,
                        source);

        for (com.yazino.platform.invitation.persistence.Invitation invitation : invitations) {
            processAcceptedInvitation(recipientPlayerId, registrationTime, invitation);
        }

        if(invitations!=null&&invitations.size()>0){
            playerReferrerEventService.send(new PlayerReferrerEvent(recipientPlayerId, PlayerReferrerEvent.INVITE, null,null));
        }

    }

    private void processAcceptedInvitation(final BigDecimal recipientPlayerId,
                                           final DateTime registrationTime,
                                           final com.yazino.platform.invitation.persistence.Invitation invitation) {
        final BigDecimal issuingPlayerId = invitation.getIssuingPlayerId();
        final BigDecimal rewardAmount = referredFriendsService.processReferral(recipientPlayerId, issuingPlayerId);
        saveChangesToRepository(invitation, rewardAmount, registrationTime);
    }

    private void saveChangesToRepository(final com.yazino.platform.invitation.persistence.Invitation invitation,
                                         final BigDecimal rewardAmount,
                                         final DateTime registrationTime) {
        invitation.marksAsAccepted(rewardAmount.longValue(), registrationTime);
        invitationRepository.save(invitation);
    }

}
