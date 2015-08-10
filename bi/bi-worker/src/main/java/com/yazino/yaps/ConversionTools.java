package com.yazino.yaps;

import java.io.UnsupportedEncodingException;
import java.util.Date;


public class ConversionTools {
    public static byte[] stringToBytes(final String s) throws InvalidFieldException {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new InvalidFieldException("No support for UTF-8 encoding!");
        }
    }

    public static byte[] hexStringToByteArray(final String s) {
        final int len = s.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(final byte[] bytes) {
        final StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            final String hex = Integer.toHexString(b);
            if (hex.length() == 1) {
                buffer.append('0');
            }
            buffer.append(hex);
        }
        return buffer.toString().replaceAll("ffffff", ""); // todo not sure why i need to do this....
    }


    public static byte[] intToByteArray(final int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    public static int bytesToInt(final byte[] bytes) {
        if (bytes.length != 4) {
            throw new IllegalArgumentException("invalid number of bytes");
        }
        int i = 0;
        i += unsignedByteToInt(bytes[0]) << 24;
        i += unsignedByteToInt(bytes[1]) << 16;
        i += unsignedByteToInt(bytes[2]) << 8;
        i += unsignedByteToInt(bytes[3]);
        return i;
    }

    public static int unsignedByteToInt(final byte b) {
        return (int) b & 0xFF;
    }

    public static int dateToSecondsSinceEpoch(final Date date) {
        final long millis = date.getTime();
        return (int) (millis / 1000);
    }

    public static byte[] dateToByteArray(final Date date) {
        return intToByteArray(dateToSecondsSinceEpoch(date));
    }


}
