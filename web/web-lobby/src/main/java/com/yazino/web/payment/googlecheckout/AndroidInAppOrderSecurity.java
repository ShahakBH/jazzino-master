package com.yazino.web.payment.googlecheckout;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import static com.yazino.platform.Partner.YAZINO;
import static java.lang.String.format;

/**
 * Responsible for verifying the data return from Android InApp Billing (i.e. purchases).
 */
@Component
public class AndroidInAppOrderSecurity {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidInAppOrderSecurity.class);

    private static final String CONFIG_PREFIX = "payment.googlecheckout.billing-key.%s.%s";
    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public AndroidInAppOrderSecurity(YazinoConfiguration yazinoConfiguration) {
        this.yazinoConfiguration = yazinoConfiguration;
    }

    /**
     * Verifies that the signature from google play matches the computed signature on {@code orderData}.
     * Returns true if the data is correctly signed.
     *
     * @param gameType  gameType purchases were made from, required to enable retrieval of correct public key
     * @param orderData signed order data from Google Play to verify
     * @param signature signature of {@code orderData} from Google Play
     * @param partnerId
     * @return true if the data and signature match
     */
    public boolean verify(String gameType, String orderData, String signature, final Partner partnerId) {
        LOG.debug("verifying gametype:{}, orderData:{}, signature:{}, partner:{}");
        validateArgs(gameType, orderData, signature);

        LOG.debug("verifying orderData {} for gameType {} against signature {}", orderData, gameType, signature);

        PublicKey publicKey = generatePublicKey(gameType, partnerId);

        Signature sig;
        try {
            sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(orderData.getBytes());
            if (!sig.verify(Base64.decode(signature.getBytes()))) {
                return false;
            }
            return true;
        } catch (Exception e) {
            LOG.error("Signature is invalid. orderData {}, gameType {}, signature {}, partnerId {}", orderData, gameType, signature, partnerId, e);
        }
        return false;
    }

    private void validateArgs(String gameType, String orderData, String signature) {
        Validate.notNull(gameType, "gameType cannot be null");
        Validate.notNull(orderData, "orderData cannot be null");
        Validate.notNull(signature, "signature cannot be null");
    }

    private PublicKey generatePublicKey(String gameType, final Partner partnerId) {
        PublicKey publicKey = null;
        String normalisedPartnerId = getNormalisedPartnerId(partnerId);
        final String licenseKey = yazinoConfiguration.getString(format(CONFIG_PREFIX, normalisedPartnerId, gameType));
        if (licenseKey == null) {
            LOG.error("No license key found for gameType {}", gameType);
        } else {
            try {
                byte[] decodedKey = Base64.decode(licenseKey.getBytes());
                KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
                publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
            } catch (Exception e) {
                LOG.error("Failed to decode licenseKey [{}] for gameType {}", licenseKey, gameType, e);
            }
        }
        return publicKey;
    }

    //need to handle the broken tokens that some players have
    private String getNormalisedPartnerId(final Partner partnerId) {
        if (partnerId == null) {
            return YAZINO.name();
        }
        return partnerId.name();
    }

}
