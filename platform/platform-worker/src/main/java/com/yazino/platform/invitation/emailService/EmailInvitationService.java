package com.yazino.platform.invitation.emailService;

import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.email.EmailValidationService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.service.InvitationTrackingService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service("emailInvitationService")
public class EmailInvitationService {
    private static final Logger LOG = LoggerFactory.getLogger(EmailInvitationService.class);

    private final String fromAddress;
    private final InvitationTrackingService invitationTrackingService;
    private final EmailValidationService emailValidationService;
    private final AsyncEmailService emailService;

    @Autowired
    public EmailInvitationService(@Value("${strata.email.from-address}") final String fromAddress,
                                  final InvitationTrackingService invitationTrackingService,
                                  final EmailValidationService emailValidationService,
                                  final AsyncEmailService emailService) {
        this.fromAddress = fromAddress;
        this.invitationTrackingService = invitationTrackingService;
        this.emailValidationService = emailValidationService;
        this.emailService = emailService;
    }

    public void sendAndTrackEmailInvitation(BigDecimal playerId,
                                            String recipientEmailAddress,
                                            DateTime timestamp,
                                            String gameType,
                                            String screenSource,
                                            EmailInvitation email) {
        boolean validRecipient = emailValidationService.validate(recipientEmailAddress);
        LOG.debug("Validation result for {}: {}", recipientEmailAddress, validRecipient);
        if (!validRecipient) {
            LOG.debug("Invalid email. Ignoring request...");
            return;
        }
        emailService.send(recipientEmailAddress, email.getSender(fromAddress), email.getSubject(),
                EmailInvitation.INVITE_FRIENDS_TEMPLATE, email.getProperties());
        invitationTrackingService.invitationSent(playerId, recipientEmailAddress, InvitationSource.EMAIL, timestamp, gameType, screenSource);
    }
}
