package com.yazino.web.service;

import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.email.EmailBuilder;
import com.yazino.web.domain.email.EmailRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang.Validate.notNull;

/**
 * This class sends emails, using a {@link com.yazino.web.domain.email.EmailBuilder} to build the email.
 * It suppresses any Exception that gets thrown from attempting to send the email.
 */
@Service("quietPlayerEmailer")
public class QuietPlayerEmailer {
    private static final Logger LOG = LoggerFactory.getLogger(QuietPlayerEmailer.class);
    static final String TEMPLATE_GROUP = "lobby/";

    private final PlayerProfileService profileService;
    private final AsyncEmailService emailService;
    private final String sender;

    @Autowired(required = true)
    public QuietPlayerEmailer(final AsyncEmailService emailService,
                              final PlayerProfileService profileService,
                              @Value("${strata.email.from-address}")final String sender) {
        notNull(emailService);
        notNull(profileService);
        notNull(sender);

        this.sender = sender;
        this.emailService = emailService;
        this.profileService = profileService;
    }

    /**
     * Send the email using the builder to build it first.
     *
     * @param builder not null
     * @return true if the email was sent, false if any Exception was thrown
     */
    public boolean quietlySendEmail(final EmailBuilder builder) {
        notNull(builder);

        final EmailRequest request = builder.buildRequest(profileService);

        return quietlySendEmail(request);
    }

    public boolean quietlySendEmail(final EmailRequest request) {
        notNull(request, "request is null");
        try {
            if (request.getAddresses() == null
                    || request.getAddresses().isEmpty()) {
                LOG.warn("Failed to send email because the address list is empty or null");
                return false;
            }
            String fromAddress = request.getFromAddress() != null ? request.getFromAddress() : sender;

            emailService.send(
                    request.getAddresses(), fromAddress, request.getSubject(), TEMPLATE_GROUP + request.getTemplate(),
                    request.getProperties());
            return true;
        } catch (Exception e) {
            final String addresses;
            if (request.getAddresses() == null) {
                addresses = "null";
            } else {
                addresses = StringUtils.join(request.getAddresses(), ", ");
            }
            LOG.warn("Failed to send email to {} because {}", addresses, e.getMessage());
        }
        return false;
    }
}
