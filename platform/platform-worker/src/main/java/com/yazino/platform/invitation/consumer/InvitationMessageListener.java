package com.yazino.platform.invitation.consumer;

import com.yazino.platform.invitation.emailService.EmailInvitation;
import com.yazino.platform.invitation.emailService.EmailInvitationService;
import com.yazino.platform.invitation.message.EmailInvitationRequestedMessage;
import com.yazino.platform.invitation.message.InvitationAcceptedMessage;
import com.yazino.platform.invitation.message.InvitationMessage;
import com.yazino.platform.invitation.message.InvitationSentMessage;
import com.yazino.platform.invitation.service.InvitationTrackingService;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

@Service("invitationsMessageListener")
public class InvitationMessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(InvitationMessageListener.class);

    private static final int MAX_SUPPORTED_VERSION = 1;

    private final InvitationTrackingService invitationTrackingService;
    private final EmailInvitationService emailInvitationService;

    @Autowired
    public InvitationMessageListener(final InvitationTrackingService invitationTrackingService,
                                     final EmailInvitationService emailInvitationService) {
        notNull(invitationTrackingService, "invitationService may not be null");
        notNull(emailInvitationService, "emailInvitationService may not be null");
        this.emailInvitationService = emailInvitationService;
        this.invitationTrackingService = invitationTrackingService;
    }

    public void handleMessage(final byte[] unconvertedMessage) {
        LOG.error(String.format("Unconverted message received, ignoring [%s]",
                ArrayUtils.toString(unconvertedMessage)));
    }

    public void handleMessage(final InvitationMessage message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Received message [%s]", message));
        }

        if (message == null) {
            return;
        }

        if (message.getVersion() > MAX_SUPPORTED_VERSION) {
            LOG.error(String.format("Received unsupported message version [%s] from message [%s]",
                    message.getVersion(), message));
            return;
        }

        if (message.getMessageType() == null) {
            LOG.error(String.format("Received message with null type [%s]", message));
            return;
        }

        switch (message.getMessageType()) {
            case EMAIL_INVITATION_REQUESTED:
                emailInvitationRequested((EmailInvitationRequestedMessage) message);
                break;

            case INVITATION_SENT:
                invitationSent((InvitationSentMessage) message);
                break;

            case INVITATION_ACCEPTED:
                invitationAccepted((InvitationAcceptedMessage) message);
                break;

            case INVITATION_NOT_ACCEPTED:
                LOG.warn("Deprecated message received: " + message);
                break;

            default:
                LOG.error("Cannot handle message type: " + message.getMessageType());
                break;
        }
    }

    private void emailInvitationRequested(EmailInvitationRequestedMessage message) {
        if (!message.isValid()) {
            LOG.error("Invalid message received: " + message);
            return;
        }

        final EmailInvitation email =
                new EmailInvitation(message.getCustomisedMessage(), message.getCallToActionUrl(), message.getIssuingPlayerName());
        emailInvitationService.sendAndTrackEmailInvitation(message.getIssuingPlayerId(), message.getRecipientEmailAddress(),
                message.getRequestTime(), message.getGameType(), message.getSource(), email);
    }

    private void invitationSent(final InvitationSentMessage message) {
        if (!message.isValid()) {
            LOG.error("Invalid message received: " + message);
            return;
        }

        invitationTrackingService.invitationSent(message.getIssuingPlayerId(), message.getRecipientIdentifier(),
                message.getSource(), message.getCreatedTime(), message.getCurrentGame(), message.getScreenSource());
    }

    private void invitationAccepted(final InvitationAcceptedMessage message) {
        if (!message.isValid()) {
            LOG.error("Invalid message received: " + message);
            return;
        }

        invitationTrackingService.invitationAccepted(message.getRecipientIdentifier(), message.getSource(),
                message.getRegistrationTime(), message.getRecipientPlayerId());
    }

}
