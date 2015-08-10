package com.yazino.platform.player.util;


import com.yazino.platform.player.PasswordType;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Hasher implements Hasher {

    @Override
    public String hash(final String value, final byte[] salt) {
        try {
            return hash(value.getBytes("UTF-8"));
        } catch (Exception exception) {
            throw new RuntimeException("Encrypting exception", exception);
        }
    }

    @Override
    public PasswordType getType() {
        return PasswordType.MD5;
    }

    @Override
    public byte[] generateSalt() {
        return null;
    }

    private static String encode(final byte[] input) throws UnsupportedEncodingException {
        return new String(Base64.encodeBase64(input, true), "UTF-8");
    }

    private static String hash(final byte[] input)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        final MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(input);

        return encode(digest.digest()).trim();
    }
}
