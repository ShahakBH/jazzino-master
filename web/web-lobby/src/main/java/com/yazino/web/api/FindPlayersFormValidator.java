package com.yazino.web.api;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.Set;

/**
 * A Validator for the {@link FindPlayersForm} object.
 * The provider's are hardcoded as we don't have an enum to represent them currently.
 */
public class FindPlayersFormValidator implements Validator {
    private static final Logger LOG = LoggerFactory.getLogger(FindPlayersFormValidator.class);

    private static final String YAZINO = "yazino";
    private static final String FACEBOOK = "facebook";

    private final Set<String> providers = new HashSet<String>();

    public FindPlayersFormValidator() {
        this.providers.add(YAZINO);
        this.providers.add(FACEBOOK);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return FindPlayersForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        FindPlayersForm form = (FindPlayersForm) target;
        ValidationTools.rejectIfEmptyOrWhitespace(errors, "provider", form.getProvider());
        if (!errors.hasFieldErrors("provider") && !providers.contains(form.getProvider().toLowerCase())) {
            ValidationTools.rejectUnsupportedValue(errors, "provider");
        }
        if (form.getProviderIds() == null || StringUtils.strip(form.getProviderIds()).length() == 0) {
            errors.rejectValue("providerIds", "empty", "providerIds must be present");
        }
        LOG.debug("Validation of {} yielded {} errors", form, errors);
    }

    boolean isYazinoProvider(String provider) {
        return YAZINO.equalsIgnoreCase(provider);
    }

    boolean isFacebookProvider(String provider) {
        return FACEBOOK.equalsIgnoreCase(provider);
    }
}
