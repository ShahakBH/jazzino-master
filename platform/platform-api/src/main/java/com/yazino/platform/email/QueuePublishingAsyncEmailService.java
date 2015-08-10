package com.yazino.platform.email;

import com.yazino.platform.email.message.EmailMessage;
import com.yazino.platform.email.message.EmailSendMessage;
import com.yazino.platform.email.message.EmailVerificationMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.Validate.*;

@Service("asyncEmailService")
public class QueuePublishingAsyncEmailService implements AsyncEmailService {
    private static final Logger LOG = LoggerFactory.getLogger(QueuePublishingAsyncEmailService.class);

    private final QueuePublishingService<EmailMessage> emailQueuePublishingService;

    @Autowired
    public QueuePublishingAsyncEmailService(@Qualifier("emailQueuePublishingService")
                                            final QueuePublishingService<EmailMessage> emailQueuePublishingService) {
        notNull(emailQueuePublishingService, "emailQueuePublishingService may not be null");

        this.emailQueuePublishingService = emailQueuePublishingService;
    }

    @Override
    public void send(final String recipient,
                     final String sender,
                     final String subject,
                     final String templateName,
                     final Map<String, Object> templateProperties) {
        notBlank(recipient, "recipient may not be blank");
        notBlank(sender, "sender may not be blank");

        send(singleton(recipient), sender, subject, templateName, templateProperties);
    }

    @Override
    public void send(final Collection<String> recipients,
                     final String sender,
                     final String subject,
                     final String templateName,
                     final Map<String, Object> templateProperties) {
        notEmpty(recipients, "recipients may not be null/empty");
        notEmpty(sender, "sender may not be null/empty");

        final EmailSendMessage message = new EmailSendMessage(recipients, sender, subject, templateName, templateProperties);
        LOG.debug("Published email send request: {}", message);
        emailQueuePublishingService.send(message);
    }

    @Override
    public void verifyAddress(final String emailAddress) {
        notEmpty(emailAddress, "emailAddress may not be null/empty");

        final EmailVerificationMessage verificationMessage = new EmailVerificationMessage(emailAddress);
        LOG.debug("Published email verification request: {}", verificationMessage);
        emailQueuePublishingService.send(verificationMessage);
    }
}
