package com.yazino.web.payment.flurry;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;

public class MobileEncryption {
    public static final String AES_ECB_PKCS7_PADDING = "AES/ECB/PKCS7Padding";
    private final Key secretKey;

    public MobileEncryption() throws UnsupportedEncodingException {
        final String keyPhrase = "abcds1gnatur3pab";
        final byte[] bytes = keyPhrase.getBytes("UTF8");
        secretKey = new SecretKeySpec(bytes, "AES");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public String encryptTicket(final String ticket)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchProviderException {
        final Cipher c = Cipher.getInstance(AES_ECB_PKCS7_PADDING, "BC");
        c.init(Cipher.ENCRYPT_MODE, secretKey);
        final byte[] encVal = c.doFinal(ticket.getBytes("UTF8"));
        return new String(Base64.encodeBase64(encVal));
    }

    public String decryptTicket(final String encrypted)
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        final byte [] b64Data = encrypted.getBytes("UTF8");
        final byte[] data = Base64.decodeBase64(b64Data);
        final Cipher c = Cipher.getInstance(AES_ECB_PKCS7_PADDING);
        c.init(Cipher.DECRYPT_MODE, secretKey);
        final byte[] decodedData = c.doFinal(data);
        return new String(decodedData);
    }
}
