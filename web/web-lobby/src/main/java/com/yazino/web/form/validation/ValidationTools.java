package com.yazino.web.form.validation;

import com.yazino.platform.player.Gender;
import com.yazino.validation.EmailAddressFormatValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides some common methods to validate fields.
 */
public final class ValidationTools {
    public static final String ERROR_CODE_NON_MATCHING = "nonMatching";
    public static final String ERROR_CODE_EMPTY = "empty";
    public static final String ERROR_CODE_T_AND_C = "missingTsAndCs";
    public static final String ERROR_CODE_LENGTH = "length";
    public static final String ERROR_CODE_INVALID = "invalid";
    public static final String ERROR_CODE_NOT_ALPHANUMERIC = "not alphanumeric";
    public static final String DEFAULT_TEXT_NON_MATCHING = "Values do not match";
    public static final String DEFAULT_TEXT_EMPTY_VALUE = "Value is required";
    public static final String DEFAULT_TEXT_MISSING_T_AND_C = "Terms and Conditions not accepted";
    public static final String DEFAULT_TEXT_NOT_ALPHANUMERIC = "Must be alphanumeric";
    public static final String DEFAULT_TEXT_INVALID_VALUE = "Value is invalid";
    public static final String VALUE_TOO_X = "Must be between %d and %d characters";
    public static final int TEN = 10;
    public static final int THREE = 3;
    public static final int ONE = 1;
    public static final int FIVE = 5;
    public static final int TWENTY = 20;

    private ValidationTools() {

    }

    public static void validateMatchingValues(final Errors errors,
                                              final String fieldName,
                                              final String valueA,
                                              final String valueB) {
        if (StringUtils.isBlank(valueA) || StringUtils.isBlank(valueB) || !valueA.equals(valueB)) {
            errors.rejectValue(fieldName, ERROR_CODE_NON_MATCHING, DEFAULT_TEXT_NON_MATCHING);
        }
    }

    public static void validateValueLength(final Errors errors,
                                           final String fieldName,
                                           final String fieldValue,
                                           final int minLength,
                                           final int maxLength) {
        if (isEmptyOrWhitespace(fieldValue)) {
            errors.rejectValue(fieldName, ERROR_CODE_EMPTY, DEFAULT_TEXT_EMPTY_VALUE);
        } else {
            final int length = fieldValue.length();
            final boolean tooShort = length < minLength;
            final boolean tooLong = length > maxLength;
            if (tooShort || tooLong) {
                final String defaultMessage = String.format(VALUE_TOO_X, minLength, maxLength);
                errors.rejectValue(fieldName, ERROR_CODE_LENGTH, defaultMessage);
            }
        }
    }

    public static boolean isEmptyOrWhitespace(final String fieldValue) {
        return StringUtils.isEmpty(fieldValue) || StringUtils.isWhitespace(fieldValue);
    }

    public static void validateDisplayName(final Errors errors,
                                           final String fieldName,
                                           final String displayName) {
        validateValueLength(errors, fieldName, displayName, THREE, TEN);
        validateIsAlphanumeric(errors, fieldName, displayName);
    }

    public static void validateGender(final Errors errors,
                                      final String fieldName,
                                      final String gender) {
        if (StringUtils.isEmpty(gender)) {
            errors.rejectValue(fieldName, ERROR_CODE_EMPTY, DEFAULT_TEXT_EMPTY_VALUE);
        } else if (Gender.getById(gender) == null) {
            errors.rejectValue(fieldName, ERROR_CODE_INVALID, DEFAULT_TEXT_INVALID_VALUE);
        }
    }

    public static void validateCountryLength(final Errors errors,
                                             final String fieldName,
                                             final String country) {
        validateValueLength(errors, fieldName, country, ONE, THREE);
    }

    public static void validateNotNull(final Errors errors,
                                       final String fieldName,
                                       final Object object) {
        if (object == null) {
            errors.rejectValue(fieldName, ERROR_CODE_EMPTY, DEFAULT_TEXT_EMPTY_VALUE);
        }
    }

    public static void validateEmailAddress(final Errors errors,
                                            final String fieldName,
                                            final String emailAddress) {
        validateValueLength(errors, fieldName, emailAddress, FIVE, EmailAddressFormatValidator.MAX_EMAIL_LENGTH);
        if (!errors.hasFieldErrors(fieldName)) {
            if (!EmailAddressFormatValidator.isValidFormat(emailAddress)) {
                errors.rejectValue(fieldName, ERROR_CODE_INVALID, DEFAULT_TEXT_INVALID_VALUE);
            }
        }
    }

    public static void validateNotEmptyOrWhitespace(final Errors errors,
                                                    final String fieldName,
                                                    final String fieldValue) {
        if (isEmptyOrWhitespace(fieldValue)) {
            errors.rejectValue(fieldName, ERROR_CODE_EMPTY, DEFAULT_TEXT_EMPTY_VALUE);
        }
    }

    public static void validatePassword(final Errors errors,
                                        final String fieldName,
                                        final String fieldValue) {
        validateValueLength(errors, fieldName, fieldValue, FIVE, TWENTY);
    }

    public static void validateTrue(final Errors errors,
                                    final String fieldName,
                                    final boolean fieldValue) {
        if (!fieldValue) {
            errors.rejectValue(fieldName, ERROR_CODE_T_AND_C, DEFAULT_TEXT_MISSING_T_AND_C);
        }
    }

    private static void validateIsAlphanumeric(final Errors errors,
                                               final String fieldName,
                                               final String value
    ) {
        if (!errors.hasFieldErrors(fieldName)) {
            final String allExceptAlphanumericAndSpaceRegex = "[^\\w\\s]";
            final Pattern p = Pattern.compile(allExceptAlphanumericAndSpaceRegex);
            final Matcher m = p.matcher(value);
            if (m.find()) {
                errors.rejectValue(fieldName, ERROR_CODE_NOT_ALPHANUMERIC, DEFAULT_TEXT_NOT_ALPHANUMERIC);
            }
        }
    }

}
