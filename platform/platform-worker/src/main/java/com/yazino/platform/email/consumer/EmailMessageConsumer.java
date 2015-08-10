package com.yazino.platform.email.consumer;

import com.yazino.email.EmailService;
import com.yazino.platform.email.EmailValidationService;
import com.yazino.platform.email.message.EmailMessage;
import com.yazino.platform.email.message.EmailSendMessage;
import com.yazino.platform.email.message.EmailVerificationMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Service("emailMessageConsumer")
public class EmailMessageConsumer implements QueueMessageConsumer<EmailMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(EmailMessageConsumer.class);

    private static final int MAX_SUPPORTED_VERSION = 1;

    private final EmailService emailService;
    private final EmailValidationService emailValidationService;

    @Autowired
    public EmailMessageConsumer(final EmailService emailService,
                                final EmailValidationService emailValidationService) {
        notNull(emailService, "emailService may not be null");
        notNull(emailValidationService, "emailValidationService may not be null");

        this.emailService = emailService;
        this.emailValidationService = emailValidationService;
    }

    @Override
    public void handle(final EmailMessage message) {
        LOG.debug("Received message [{}]", message);

        if (message == null) {
            return;
        }

        if (message.getVersion() > MAX_SUPPORTED_VERSION) {
            LOG.error("Received unsupported message version [{}] from message [{}]",
                    message.getVersion(), message);
            return;
        }

        if (message instanceof EmailSendMessage) {
            sendEmail((EmailSendMessage) message);

        } else if (message instanceof EmailVerificationMessage) {
            verifyEmail((EmailVerificationMessage) message);

        } else {
            LOG.error("Unknown message received: {}", message);
        }
    }

    private void verifyEmail(final EmailVerificationMessage message) {
        try {
            emailValidationService.validate(message.getEmailAddress());

        } catch (Exception e) {
            LOG.error("Email verification failed for address: {}", message.getEmailAddress(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendEmail(final EmailSendMessage message) {
        try {
            final List<String> filteredRecipients = findValidRecipientsFrom(message.getRecipients());

            if (!filteredRecipients.isEmpty()) {
                LOG.debug("Sending email to {}; from {} with subject {}", message.getRecipients(), message.getSender(), message.getSubject());

                emailService.send(filteredRecipients.toArray(new String[filteredRecipients.size()]),
                        message.getSender(),
                        message.getSubject(),
                        message.getTemplateName(),
                        message.getTemplateProperties());
            }

        } catch (Exception e) {
            LOG.error("Email send failed for email to {}; from {}; subject {}; templateName {}",
                    message.getRecipients(), message.getSender(), message.getSubject(), message.getTemplateName(), e);
        }
    }

    private List<String> findValidRecipientsFrom(final Collection<String> recipients) {
        final List<String> filteredRecipients = new ArrayList<String>();
        if (recipients != null) {
            for (String recipient : recipients) {
                if (emailValidationService.validate(recipient)) {
                    filteredRecipients.add(recipient);
                } else {
                    LOG.info("Invalid recipient removed: {}", recipient);
                }
            }
        }
        return filteredRecipients;
    }


}
