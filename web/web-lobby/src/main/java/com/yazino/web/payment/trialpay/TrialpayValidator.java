package com.yazino.web.payment.trialpay;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class TrialpayValidator {

    private static final Logger LOG = LoggerFactory.getLogger(TrialpayValidator.class);

    private String notificationKey;

    @Autowired
    public TrialpayValidator(@Value("${strata.server.lobby.trialpay.notificationKey}") final String notificationKey) {
        this.notificationKey = notificationKey;
    }

    public boolean validate(final String expectedHash, final String test) {
        try {

            LOG.info("Trying to validate hash[" + expectedHash + "]");

            final SecretKeySpec secretKeySpec = new SecretKeySpec(notificationKey.getBytes("UTF-8"), "hmacMD5");
            final Mac mac = Mac.getInstance(secretKeySpec.getAlgorithm());
            mac.init(secretKeySpec);

            final byte[] hashBytes = mac.doFinal(test.getBytes("UTF-8"));
            final String actualHash = new String(Hex.encodeHex(hashBytes));

            LOG.info("Actual hash[" + actualHash + "]");

            return (actualHash.equals(expectedHash)); // returns true if validation is successful.

        } catch (Exception e) {
            LOG.error("Validation failed for hash {}", expectedHash, e);
        }
        return false;
    }


}
