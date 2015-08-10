package com.google.android.gcm.server;

public class ResultFactory {

    /*
        The only way to construct an instance of this class is via the package-scoped Builder class...
     */
    public static Result createResult(String errorCode, String messageId, String canonicalRegistrationId) {
        return new Result.Builder()
                .errorCode(errorCode)
                .messageId(messageId)
                .canonicalRegistrationId(canonicalRegistrationId)
                .build();
    }
}
