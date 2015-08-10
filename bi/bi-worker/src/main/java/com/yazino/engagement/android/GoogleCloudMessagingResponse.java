package com.yazino.engagement.android;

public class GoogleCloudMessagingResponse {

    private String errorCode;
    private String messageId;
    private String canonicalRegistrationId;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCanonicalRegistrationId() {
        return canonicalRegistrationId;
    }

    public void setCanonicalRegistrationId(String canonicalRegistrationId) {
        this.canonicalRegistrationId = canonicalRegistrationId;
    }

    @Override
    public String toString() {
        return "GoogleCloudMessagingResponse{"
                + "errorCode='" + errorCode + '\''
                + ", messageId='" + messageId + '\''
                + ", canonicalRegistrationId='" + canonicalRegistrationId + '\''
                + '}';
    }
}
