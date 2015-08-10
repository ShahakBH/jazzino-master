package com.yazino.validation;

import java.util.regex.Pattern;

public final class EmailAddressFormatValidator {

    public static final int MAX_EMAIL_LENGTH = 50;

    // Highly relaxed validation - we want to allow '+' characters and
    private static final Pattern VALID_EMAIL_REGEX = Pattern.compile("^.+@.+\\..+$");

    private EmailAddressFormatValidator() {
    }

    public static boolean isValidFormat(String value) {
        return VALID_EMAIL_REGEX.matcher(value).matches();
    }

}
