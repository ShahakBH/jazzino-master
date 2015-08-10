package com.yazino.platform.player.validation;

import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.community.ProfanityFilter;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.persistence.YazinoLoginDao;
import com.yazino.validation.EmailAddressFormatValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.Validate.notNull;

@Service("yazinoPlayerValidator")
public class YazinoPlayerValidator implements Serializable {
    private static final long serialVersionUID = -6745342824777555789L;

    private static final Logger LOG = LoggerFactory.getLogger(YazinoPlayerValidator.class);

    private static final String DISPLAY_NAME_IS_OFFENSIVE = "Display name is offensive";
    private static final String EMAIL_MISSING = "Email must be entered";
    private static final String PASSWORD_MISSING = "Password must be entered";
    private static final String DISPLAY_NAME_MISSING = "Display name must be entered";
    private static final String PICTURE_LOCATION_MISSING = "Avatar must be selected";
    private static final int MIN_PASSWORD_LENGTH = 5;
    private static final int MAX_PASSWORD_LENGTH = 20;
    private static final int MIN_DISPLAY_NAME_LENGTH = 3;
    private static final int MAX_DISPLAY_NAME_LENGTH = 10;
    private static final String INVALID_PASSWORD_LENGTH = String.format(
            "Password must be %s-%s characters long", MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH);
    private static final String INVALID_DISPLAY_NAME_LENGTH = String.format(
            "Display name must be %s-%s characters long", MIN_DISPLAY_NAME_LENGTH, MAX_DISPLAY_NAME_LENGTH);
    private static final String DISPLAY_NAME_NOT_ALPHANUMERIC = "Display name must be alphanumeric";
    private static final String INVALID_EMAIL_FORMAT = "E-mail address does not appear to be valid";
    private static final String CONVERSION_EMAIL_ADDRESS_NOT_UNIQUE = "Cannot convert to Yazino account. E-mail already registered";

    private final YazinoLoginDao yazinoLoginDao;
    private final ProfanityFilter profanityFilter;

    @Autowired
    public YazinoPlayerValidator(final YazinoLoginDao yazinoLoginDao,
                                 final ProfanityFilter profanityFilter) {
        notNull(yazinoLoginDao, "yazinoLoginDao may not be null");
        notNull(profanityFilter, "profanityFilter may not be null");
        this.yazinoLoginDao = yazinoLoginDao;
        this.profanityFilter = profanityFilter;
    }

    public Set<ParameterisedMessage> validate(final String email,
                                              final String password,
                                              final PlayerProfile profile,
                                              final String avatarUrl,
                                              final Boolean passwordRequired) {
        notNull(profile, "profile must not be null");

        final Set<ParameterisedMessage> messages = new HashSet<ParameterisedMessage>();

        checkNotBlank(email, EMAIL_MISSING, messages);
        checkNotBlank(profile.getEmailAddress(), EMAIL_MISSING, messages);
        checkNotBlank(profile.getDisplayName(), DISPLAY_NAME_MISSING, messages);
        checkNotBlank(avatarUrl, PICTURE_LOCATION_MISSING, messages);
        if (passwordRequired) {
            checkNotBlank(password, PASSWORD_MISSING, messages);
        }

        if (StringUtils.isNotBlank(profile.getEmailAddress())) {
            checkEmailFormat(profile.getEmailAddress(), messages);
        }

        if (StringUtils.isNotBlank(profile.getDisplayName())) {
            if (checkIsDisplayNameAlphanumeric(profile.getDisplayName(), DISPLAY_NAME_NOT_ALPHANUMERIC, messages)) {
                checkLength(profile.getDisplayName(), MIN_DISPLAY_NAME_LENGTH,
                        MAX_DISPLAY_NAME_LENGTH, INVALID_DISPLAY_NAME_LENGTH, messages);
            }
        }

        if (StringUtils.isNotBlank(password)) {
            checkLength(password, MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH, INVALID_PASSWORD_LENGTH, messages);
        }

        if (!StringUtils.isBlank(profile.getDisplayName())) {
            final String filtered = profanityFilter.filter(profile.getDisplayName());
            if (filtered == null || !filtered.equals(profile.getDisplayName())) {
                messages.add(new ParameterisedMessage(DISPLAY_NAME_IS_OFFENSIVE));
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Returning validation results: {}", messages);
        }

        return messages;
    }

    public Set<ParameterisedMessage> validateForYazinoConversion(final String displayName,
                                                                 final String email,
                                                                 final String password) {
        final Set<ParameterisedMessage> messages = new HashSet<ParameterisedMessage>();

        checkNotBlank(email, EMAIL_MISSING, messages);
        checkNotBlank(displayName, DISPLAY_NAME_MISSING, messages);
        checkNotBlank(password, PASSWORD_MISSING, messages);

        if (StringUtils.isNotBlank(email)) {
            checkEmailFormat(email, messages); // TODO don't do the next check if format fails
            final boolean exists = yazinoLoginDao.existsWithEmailAddress(email.toLowerCase());
            if (exists) {
                messages.add(new ParameterisedMessage(CONVERSION_EMAIL_ADDRESS_NOT_UNIQUE));
            }
        }

        if (StringUtils.isNotBlank(displayName)) {
            if (checkIsDisplayNameAlphanumeric(displayName, DISPLAY_NAME_NOT_ALPHANUMERIC, messages)) {
                checkLength(displayName, MIN_DISPLAY_NAME_LENGTH,
                        MAX_DISPLAY_NAME_LENGTH, INVALID_DISPLAY_NAME_LENGTH, messages);
            }
            String filtered = profanityFilter.filter(displayName);
            if (filtered == null || !filtered.equals(displayName)) {
                messages.add(new ParameterisedMessage(DISPLAY_NAME_IS_OFFENSIVE));
            }
        }

        if (StringUtils.isNotBlank(password)) {
            checkLength(password, MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH, INVALID_PASSWORD_LENGTH, messages);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Returning validation results: {}", messages);
        }

        return messages;
    }

    private boolean checkNotBlank(final String value,
                                  final String message,
                                  final Set<ParameterisedMessage> messages) {
        if (StringUtils.isBlank(value)) {
            messages.add(new ParameterisedMessage(message));
            return false;
        }
        return true;
    }

    private void checkLength(final String value,
                             final int minLength,
                             final int maxLength,
                             final String message,
                             final Set<ParameterisedMessage> messages) {
        if (value.length() < minLength || value.length() > maxLength) {
            messages.add(new ParameterisedMessage(message));
        }
    }

    private boolean checkIsDisplayNameAlphanumeric(final String value,
                                                   final String message,
                                                   final Set<ParameterisedMessage> messages) {
        final String allExceptAlphanumericAndSpaceRegex = "[^\\w\\s]";
        return findUnwantedCharacters(value, message, messages, allExceptAlphanumericAndSpaceRegex);
    }

    private boolean findUnwantedCharacters(final String value,
                                           final String message,
                                           final Set<ParameterisedMessage> messages,
                                           final String unwantedCharsRegex) {
        final Pattern p = Pattern.compile(unwantedCharsRegex);
        final Matcher m = p.matcher(value);
        if (m.find()) {
            messages.add(new ParameterisedMessage(message));
            return false;
        }
        return true;
    }

    private void checkEmailFormat(final String value,
                                  final Set<ParameterisedMessage> messages) {
        if (value.length() > EmailAddressFormatValidator.MAX_EMAIL_LENGTH) {
            messages.add(new ParameterisedMessage(INVALID_EMAIL_FORMAT));
            return;
        }

        if (!EmailAddressFormatValidator.isValidFormat(value)) {
            messages.add(new ParameterisedMessage(INVALID_EMAIL_FORMAT));
        }
    }
}
