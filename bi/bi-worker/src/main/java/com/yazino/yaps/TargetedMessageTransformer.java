package com.yazino.yaps;

import com.yazino.mobile.yaps.message.PushMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.yazino.yaps.ConversionTools.intToByteArray;

/**
 * Transforms a {@link com.yazino.mobile.yaps.message.PushMessage} to a series of bytes.
 */
public class TargetedMessageTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(TargetedMessageTransformer.class);

    private JsonHelper jsonHelper = new JsonHelper(true);

    public byte[] toBytes(final TargetedMessage targeted) throws MessageTransformationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Transforming [%s] into bytes", targeted));
        }

        final PushMessageBinaryBuilder builder = new PushMessageBinaryBuilder();
        final PushMessage message = targeted.getMessage();
        try {
            builder.setDeviceToken(targeted.getDeviceToken());
            builder.setIdentifier(intToByteArray(message.getPlayerId().intValue()));
            builder.setExpiryDate(message.getExpiryDateSecondsSinceEpoch());
            builder.setPayload(toPayload(message));

            final byte[] bytes = builder.toBytes();
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Transformed [%s] to %s", targeted, new String(bytes)));
            }
            return bytes;
        } catch (InvalidFieldException e) {
            throw new MessageTransformationException(e);
        }
    }

    private String toPayload(final PushMessage message) {
        final Map<String, Object> payload = new HashMap<String, Object>();
        if (message.getAlert() != null) {
            payload.put("alert", message.getAlert());
        }
        if (message.getSound() != null) {
            payload.put("sound", message.getSound());
        }
        if (message.getBadge() != null) {
            payload.put("badge", message.getBadge());
        }

        final Map<String, Object> aps = new HashMap<String, Object>();
        aps.put("aps", payload);
        return jsonHelper.serialize(aps);
    }

}
