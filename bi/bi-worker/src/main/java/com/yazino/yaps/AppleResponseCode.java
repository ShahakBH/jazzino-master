package com.yazino.yaps;

/**
 * This class
 */
public enum AppleResponseCode {
    NoErrorsEncountered(0),
    ProcessingError(1),
    MissingDeviceToken(2),
    MissingTopic(3),
    MissingPayload(4),
    InvalidTokenSize(5),
    InvalidTopicSize(6),
    InvalidPayloadSize(7),
    InvalidToken(8),
    None(255);

    public static AppleResponseCode responseCode(final int code) {
        if (code == 255) {
            return None;
        }
        return values()[code];
    }

    private final int code;

    private AppleResponseCode(final int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        switch (code) {
            case 0:
                return "NoErrorsEncountered";
            case 1:
                return "ProcessingError";
            case 2:
                return "MissingDeviceToken";
            case 3:
                return "MissingTopic";
            case 4:
                return "MissingPayload";
            case 5:
                return "InvalidTokenSize";
            case 6:
                return "InvalidTopicSize";
            case 7:
                return "InvalidPayloadSize";
            case 8:
                return "InvalidToken";
            default:
                return "None";
        }
    }
}
