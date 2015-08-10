package com.yazino.platform.invitation;

import com.yazino.platform.invitation.message.EmailInvitationRequestedMessage;
import com.yazino.platform.invitation.message.InvitationAcceptedMessage;
import com.yazino.platform.invitation.message.InvitationSentMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class QueuePublishingInvitationService implements InvitationService {
    private final QueuePublishingService queuePublishingService;

    @Autowired(required = true)
    public QueuePublishingInvitationService(final QueuePublishingService queuePublishingService) {
        notNull(queuePublishingService, "queuePublishingService may not be null");

        this.queuePublishingService = queuePublishingService;
    }

    @Override
    public void sendEmailInvitation(final BigDecimal issuingPlayerId,
                                    final String issuingPlayerName,
                                    final String recipientEmail,
                                    final String customisedMessage,
                                    final String callToActionUrl,
                                    final DateTime requestTime,
                                    final String gameType,
                                    final String screenSource) {
        notNull(issuingPlayerId, "issuingPlayerId may not be null");
        notBlank(issuingPlayerName, "issuingPlayerName may not be null");
        notBlank(recipientEmail, "recipientEmail may not be null");
        notNull(requestTime, "requestTime may not be null");

        queuePublishingService.send(new EmailInvitationRequestedMessage(issuingPlayerId, issuingPlayerName,
                recipientEmail, customisedMessage, callToActionUrl, requestTime, gameType, screenSource));
    }

    @Override
    public void invitationSent(final BigDecimal issuingPlayerId,
                               final String recipientIdentifier,
                               final InvitationSource source,
                               final DateTime createdTime,
                               final String currentGame,
                               final String screenSource) {
        notNull(issuingPlayerId, "issuingPlayerId may not be null");
        notBlank(recipientIdentifier, "recipientIdentifier may not be null");
        notNull(source, "source may not be null");
        notNull(createdTime, "createdTime may not be null");

        queuePublishingService.send(new InvitationSentMessage(issuingPlayerId, recipientIdentifier, source,
                createdTime, currentGame, screenSource));
    }

    @Override
    public void invitationAccepted(final String recipientIdentifier,
                                   final InvitationSource source,
                                   final DateTime registrationTime,
                                   final BigDecimal recipientPlayerId) {
        notBlank(recipientIdentifier, "recipientIdentifier may not be null");
        notNull(source, "source may not be null");
        notNull(registrationTime, "registrationTime may not be null");
        notNull(recipientPlayerId, "recipientPlayerId may not be null");

        queuePublishingService.send(new InvitationAcceptedMessage(
                recipientIdentifier, source, registrationTime, recipientPlayerId));

    }

}
