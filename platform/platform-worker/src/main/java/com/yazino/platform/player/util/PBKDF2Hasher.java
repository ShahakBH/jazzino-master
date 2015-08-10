package com.yazino.platform.player.util;

import com.yazino.platform.player.PasswordType;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import static org.apache.commons.lang3.Validate.notNull;

public class PBKDF2Hasher implements Hasher {

    private final PasswordType passwordType;
    private final int iterations;
    private final int saltLengthInBytes;
    private final int derivedKeyLength;

    public PBKDF2Hasher(final PasswordType passwordType,
                        final int iterations,
                        final int saltLengthInBytes,
                        final int derivedKeyLength) {
        notNull(passwordType, "passwordType may not be null");

        this.passwordType = passwordType;
        this.iterations = iterations;
        this.saltLengthInBytes = saltLengthInBytes;
        this.derivedKeyLength = derivedKeyLength;
    }

    @Override
    public PasswordType getType() {
        return passwordType;
    }

    @Override
    public String hash(final String value,
                       final byte[] salt) {
        if (value == null) {
            return null;
        }

        try {
            final KeySpec spec = new PBEKeySpec(value.toCharArray(), salt, iterations, derivedKeyLength);
            final SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return new String(Base64.encodeBase64(f.generateSecret(spec).getEncoded(), true), "UTF-8").trim();

        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    @Override
    public byte[] generateSalt() {
        try {
            final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

            final byte[] salt = new byte[saltLengthInBytes];
            random.nextBytes(salt);

            return salt;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA1PRNG is not supported by JVM");
        }
    }
}
