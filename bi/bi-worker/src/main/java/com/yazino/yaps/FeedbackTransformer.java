package com.yazino.yaps;

import java.util.Date;

import static com.yazino.yaps.ConversionTools.byteArrayToHexString;
import static com.yazino.yaps.ConversionTools.bytesToInt;

/**
 * Transforms a series of bytes into a feedback object.
 */
public class FeedbackTransformer {

    public Feedback fromBytes(final byte[] bytes) throws MessageTransformationException {
        if (bytes.length != 38) {
            throw new MessageTransformationException(String.format(
                    "Not enough bytes, expected [%d] was [%d]", 38, bytes.length));
        }
        final byte[] dateBytes = new byte[4];
        System.arraycopy(bytes, 0, dateBytes, 0, 4);
        final int secondsSinceEpoch = bytesToInt(dateBytes);

        final byte[] deviceTokenBytes = new byte[32];
        System.arraycopy(bytes, 6, deviceTokenBytes, 0, 32);

        final String deviceToken = byteArrayToHexString(deviceTokenBytes);
        return new Feedback(deviceToken, new Date(secondsSinceEpoch * 1000L));
    }
}
