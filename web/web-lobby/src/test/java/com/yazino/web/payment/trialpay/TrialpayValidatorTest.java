package com.yazino.web.payment.trialpay;

import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrialpayValidatorTest {

    TrialpayValidator trialPayValidationService;
    private String notificationKey;

    @Before
    public void before() {
        notificationKey = "b478928959";
        trialPayValidationService = new TrialpayValidator(notificationKey);
    }

    @Test
    public void shouldValidateSignature() throws IOException {
        assertTrue(trialPayValidationService.validate("58b950047a831240216f0db6a2cd1c66", "oid=CW89CWY&sid=1940&reward_amount=1"));
    }

    @Test
    public void shouldNotValidateInvalidExpectedHash() throws IOException {
        assertFalse(trialPayValidationService.validate("58b950047a831240216f0db6a2cd1c667", "oid=CW89CWY&sid=1940&reward_amount=1"));
    }

    @Test
    public void shouldNotValidateInvalidContent() throws IOException {
        assertFalse(trialPayValidationService.validate("58b950047a831240216f0db6a2cd1c666", "oid=CW89CWY&sid=1940&reward_amount=2"));
    }

    @Test
    public void generateSomeHashcodesHere() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String test = "oid=TEST&sid=111&reward_amount=1&revenue=10";
        final SecretKeySpec secretKeySpec = new SecretKeySpec(notificationKey.getBytes("UTF-8"), "hmacMD5");
        final Mac mac = Mac.getInstance(secretKeySpec.getAlgorithm());
        mac.init(secretKeySpec);
        final byte[] hashBytes = mac.doFinal(test.getBytes("UTF-8"));
        final String actualHash = new String(Hex.encodeHex(hashBytes));
        assertTrue(trialPayValidationService.validate(actualHash, test));

        System.out.println("Hash:" + actualHash);

    }

}
