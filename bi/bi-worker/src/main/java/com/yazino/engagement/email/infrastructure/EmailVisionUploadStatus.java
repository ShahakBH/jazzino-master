package com.yazino.engagement.email.infrastructure;

public enum EmailVisionUploadStatus {

    STORAGE(UploadStatusType.PENDING),
    VALIDATED(UploadStatusType.PENDING),
    QUEUED(UploadStatusType.PENDING),
    IMPORTING(UploadStatusType.PENDING),
    ERROR(UploadStatusType.ERROR),
    FAILURE(UploadStatusType.ERROR),
    DONE(UploadStatusType.SUCCESS),
    DONE_WITH_ERRORS(UploadStatusType.SUCCESS);


    private enum UploadStatusType {
        ERROR,
        PENDING,
        SUCCESS;
    }
    private final UploadStatusType type;

    EmailVisionUploadStatus(UploadStatusType type) {
        this.type = type;
    }

    public boolean isError() {
        return type.equals(UploadStatusType.ERROR);
    }

    public boolean isPending() {
        return type.equals(UploadStatusType.PENDING);
    }

    public boolean isSuccess() {
        return type.equals(UploadStatusType.SUCCESS);
    }

    public static EmailVisionUploadStatus getStatus(final String evResponse) {

        if ("DONE WITH ERROR(S)".equalsIgnoreCase(evResponse)) {
            return EmailVisionUploadStatus.DONE_WITH_ERRORS;
        } else {
            return EmailVisionUploadStatus.valueOf(evResponse);
        }
    }
}
