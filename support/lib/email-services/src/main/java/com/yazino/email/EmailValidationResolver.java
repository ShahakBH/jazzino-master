package com.yazino.email;

import com.yazino.configuration.YazinoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

@Service("emailValidationResolver")
public class EmailValidationResolver {
    private static final Logger LOG = LoggerFactory.getLogger(EmailValidationResolver.class);

    private static final String PROPERTY_ALLOW_UNKNOWN = "email.allow-unknown";
    private static final String PROPERTY_ALLOW_ACCEPT_ALL = "email.allow-accept-all";
    private static final String PROPERTY_ALLOW_SERVICE_ERROR = "email.allow-service-error";
    private static final String PROPERTY_ALLOW_DISPOSABLE = "email.allow-disposable";
    private static final String PROPERTY_ALLOW_ROLE = "email.allow-role";

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public EmailValidationResolver(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    public boolean isValid(final EmailVerificationResult result) {
        notNull(result, "result may not be null");

        boolean valid = true;
        if (result.isDisposable() && !yazinoConfiguration.getBoolean(PROPERTY_ALLOW_DISPOSABLE, false)) {
            LOG.debug("Address is disposable and we do not allow disposable addresses: {}", result.getAddress());
            valid = false;
        }

        if (result.isRole() &&  !yazinoConfiguration.getBoolean(PROPERTY_ALLOW_ROLE, false)) {
            LOG.debug("Address is a role and we do not allow role addresses: {}", result.getAddress());
            valid = false;
        }

        switch (result.getStatus()) {
            case MALFORMED:
            case INVALID:
                return false;

            case UNKNOWN_TEMPORARY:
                return valid && yazinoConfiguration.getBoolean(PROPERTY_ALLOW_SERVICE_ERROR, true);

            case UNKNOWN:
                return valid && yazinoConfiguration.getBoolean(PROPERTY_ALLOW_UNKNOWN, true);

            case ACCEPT_ALL:
                return valid && yazinoConfiguration.getBoolean(PROPERTY_ALLOW_ACCEPT_ALL, true);

            case VALID:
                return valid;

            default:
                throw new IllegalArgumentException("Unknown status: " + result);
        }

    }
}
