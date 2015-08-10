package com.yazino.yaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Transforms a series of bytes to a {@link PushResponse} object.
 * The response from apple has the form |command(1 byte)|status(1)|identifier/playerId(4)|
 */
public class PushResponseTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(PushResponseTransformer.class);

    public PushResponse fromBytes(final byte[] bytes) throws MessageTransformationException {
        if (bytes.length != 6) {
            throw new MessageTransformationException(String.format(
                    "Invalid bytes length was [%d], expected [%d]", bytes.length, 6));
        }

        final AppleResponseCode code = AppleResponseCode.responseCode(bytes[1]);
        final byte[] playerIdBytes = new byte[4];
        System.arraycopy(bytes, 2, playerIdBytes, 0, 4);
        final int playerId = ConversionTools.bytesToInt(playerIdBytes);

        final PushResponse response = new PushResponse(code, BigDecimal.valueOf(playerId));
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Transformed [%s] to %s", new String(bytes), response));
        }
        return response;
    }

}
