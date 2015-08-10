package com.yazino.web.util;

import com.google.common.base.Charsets;
import com.yazino.configuration.YazinoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class SymmetricEncryptor {
    private static final String CYPHER = "AES/CBC/PKCS5Padding";
    private static final String KEY_CYPHER = "AES";
    private static final String PROPERTY_KEY = "client.auth.key";
    private static final String PROPERTY_IV = "client.auth.iv";

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public SymmetricEncryptor(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    public String encrypt(final String plainTextValue) throws GeneralSecurityException {
        if (plainTextValue == null) {
            return null;
        }

        return new String(Base64.encode(encrypt(yazinoConfiguration.getString(PROPERTY_KEY),
                yazinoConfiguration.getString(PROPERTY_IV).getBytes(Charsets.UTF_8),
                plainTextValue)), Charsets.UTF_8);
    }

    public String decrypt(final String base64EncryptedValue) throws GeneralSecurityException {
        if (base64EncryptedValue == null) {
            return null;
        }

        return decrypt(yazinoConfiguration.getString(PROPERTY_KEY),
                yazinoConfiguration.getString(PROPERTY_IV).getBytes(Charsets.UTF_8),
                Base64.decode(base64EncryptedValue.getBytes(Charsets.UTF_8)));
    }

    private byte[] encrypt(final String key,
                           final byte[] initVector,
                           final String value) {
        try {
            final Cipher cypher = Cipher.getInstance(CYPHER);
            cypher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(Charsets.UTF_8), KEY_CYPHER), new IvParameterSpec(initVector));
            return cypher.doFinal(value.getBytes(Charsets.UTF_8));

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt", e);
        }
    }

    private String decrypt(final String key,
                           final byte[] initVector,
                           final byte[] encrypted) {
        try {
            final Cipher cypher = Cipher.getInstance(CYPHER);
            cypher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(Charsets.UTF_8), KEY_CYPHER), new IvParameterSpec(initVector));
            final byte[] original = cypher.doFinal(encrypted);
            return new String(original, Charsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt", e);
        }
    }
}
