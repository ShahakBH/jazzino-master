package com.yazino.yaps;

import static com.yazino.yaps.ConversionTools.*;

/**
 * Builds the binary byte[] array which gets send to apple.
 * Performs any validation on the input as required.
 */
public class PushMessageBinaryBuilder {
    static final int MAX_LENGTH = 301;

    private byte[] buffer = new byte[MAX_LENGTH];

    public PushMessageBinaryBuilder() {
        buffer[Component.Command.start] = 1;
        buffer[Component.TokenStart.start] = 0;
        buffer[Component.TokenLength.start] = 32;
        buffer[Component.PayloadStart.start] = 0;
    }

    public PushMessageBinaryBuilder setExpiryDate(final int secondsSinceEpoch) throws InvalidFieldException {
        final byte[] bytes = intToByteArray(secondsSinceEpoch);
        copyIntoWithLengthValidation(Component.Expiry, bytes, buffer);
        return this;
    }

    public PushMessageBinaryBuilder setIdentifier(final byte[] identifier) throws InvalidFieldException {
        copyIntoWithLengthValidation(Component.Identifier, identifier, buffer);
        return this;
    }

    public PushMessageBinaryBuilder setDeviceToken(final String deviceToken) throws InvalidFieldException {
        // todo a bit hacky
        final byte[] bytes = hexStringToByteArray(deviceToken.replaceAll(" ", "")
                .replaceAll("<", "").replaceAll(">", ""));
        copyIntoWithLengthValidation(Component.Token, bytes, buffer);
        return this;
    }

    public PushMessageBinaryBuilder setPayload(final String payload) throws InvalidFieldException {
        final byte[] bytes = stringToBytes(payload);
        if (bytes.length > 256) {
            throw new InvalidFieldException(String.format("payload must be < %d bytes, was %d", 256, bytes.length));
        }
        copyInto(bytes, buffer, Component.Payload.start);
        buffer[Component.PayloadLength.start] = (byte) bytes.length;
        final byte[] copy = new byte[Component.Payload.start + bytes.length];
        copyInto(buffer, copy, 0, copy.length);
        buffer = copy;
        return this;
    }

    public byte[] toBytes() {
        return buffer;
    }

    private static void copyIntoWithLengthValidation(final Component component,
                                                     final byte[] source,
                                                     final byte[] target) throws InvalidFieldException {
        if (source.length != component.length) {
            throw new InvalidFieldException(component.length, source.length);
        }
        copyInto(source, target, component.start);
    }

    private static void copyInto(final byte[] source,
                                 final byte[] target,
                                 final int start) throws InvalidFieldException {
        copyInto(source, target, start, source.length);
    }

    private static void copyInto(final byte[] source,
                                 final byte[] target,
                                 final int start,
                                 final int length) throws InvalidFieldException {
        try {
            System.arraycopy(source, 0, target, start, length);
        } catch (Exception e) {
            throw new InvalidFieldException(e);
        }
    }


    private enum Component {
        Command(0, 1),
        Identifier(1, 4),
        Expiry(5, 4),
        TokenStart(9, 1),
        TokenLength(10, 1),
        Token(11, 32),
        PayloadStart(43, 1),
        PayloadLength(44, 1),
        Payload(45);

        private final int start, length;

        private Component(final int start,
                          final int length) {
            this.start = start;
            this.length = length;
        }

        private Component(final int start) {
            this.start = start;
            this.length = -1;
        }

    }
}

