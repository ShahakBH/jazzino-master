package com.yazino.platform.email;

import com.google.common.base.Optional;
import com.yazino.email.EmailValidationResolver;
import com.yazino.email.EmailValidator;
import com.yazino.email.EmailVerificationResult;
import com.yazino.platform.email.persistence.JDBCEmailValidationDAO;
import com.yazino.platform.event.message.EmailValidationEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.yazino.email.EmailVerificationStatus.UNKNOWN_TEMPORARY;
import static org.apache.commons.lang3.Validate.notNull;

@Service("emailValidationService")
public class CachingEmailValidationService implements EmailValidationService {
    private static final Logger LOG = LoggerFactory.getLogger(CachingEmailValidationService.class);

    private final EmailValidationResolver emailValidationResolver;
    private final EmailValidator emailValidator;
    private final JDBCEmailValidationDAO emailValidationDao;
    private final QueuePublishingService<EmailValidationEvent> emailValidationEventQueuePublishingService;

    @Autowired
    public CachingEmailValidationService(final EmailValidationResolver emailValidationResolver,
                                         final EmailValidator emailValidator,
                                         final JDBCEmailValidationDAO emailValidationDao,
                                         final QueuePublishingService<EmailValidationEvent> emailValidationEventQueuePublishingService) {
        notNull(emailValidator, "emailValidator may not be null");
        notNull(emailValidationResolver, "emailValidationResolver may not be null");
        notNull(emailValidationDao, "emailValidationDao may not be null");
        notNull(emailValidationEventQueuePublishingService, "emailValidationEventQueuePublishingService may not be null");

        this.emailValidationResolver = emailValidationResolver;
        this.emailValidator = emailValidator;
        this.emailValidationDao = emailValidationDao;
        this.emailValidationEventQueuePublishingService = emailValidationEventQueuePublishingService;
    }

    @Override
    public boolean validate(final String emailAddress) {
        return emailAddress != null && emailValidationResolver.isValid(validationResultFor(emailAddress));
    }

    private EmailVerificationResult validationResultFor(final String emailAddress) {
        final Optional<EmailVerificationResult> validationResult = emailValidationDao.findByAddress(emailAddress);
        if (validationResult.isPresent() && validationResult.get().getStatus() != UNKNOWN_TEMPORARY) {
            LOG.debug("Email is cached in DB, returning status: {}", validationResult.get());
            return validationResult.get();
        }

        LOG.debug("Email is not cached or has temporary status, updating: {}", emailAddress);
        final EmailVerificationResult remoteResult = emailValidator.validate(emailAddress);
        emailValidationDao.save(remoteResult);
        emailValidationEventQueuePublishingService.send(new EmailValidationEvent(remoteResult.getAddress(), remoteResult.getStatus().getId()));
        return remoteResult;
    }
}
