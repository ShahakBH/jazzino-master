package com.yazino.email;

public interface EmailValidator {

    /**
     * Validate an email address.
     *
     * @param address the address to verify. Not null.
     * @return the validation result.
     */
    EmailVerificationResult validate(final String address);

}
