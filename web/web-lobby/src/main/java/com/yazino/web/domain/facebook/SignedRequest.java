package com.yazino.web.domain.facebook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Facebook Signed Request handling.
 * <p/>
 *
 * @see "http://developers.facebook.com/docs/guides/canvas/#auth"
 */
public class SignedRequest {
    private static final Logger LOG = LoggerFactory.getLogger(SignedRequest.class);

    private final Map<String, Object> properties;

    public SignedRequest(final String signedRequest,
                         final String appSecret) {
        notNull(signedRequest, "signedRequest may not be null");
        notNull(appSecret, "appSecret may not be null");

        properties = decodeSignedRequest(signedRequest, appSecret);
    }

    public String getOAuthToken() {
        return (String) properties.get("oauth_token");
    }

    public Object get(final String key) {
        return properties.get(key);
    }

    public int size() {
        return properties.size();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeSignedRequest(final String signedRequest,
                                                    final String appSecret) {
        if (signedRequest == null) {
            LOG.warn("Invalid signature: " + signedRequest);
            return Collections.emptyMap();
        }

        final String[] parts = signedRequest.split("\\.");
        if (parts.length != 2) {
            LOG.warn("Invalid signature: " + signedRequest);
            return Collections.emptyMap();
        }

        final String rawSignature = parts[0];
        final String rawPayload = parts[1];

        Map<String, Object> data;
        final String decodedJson = asString(fromBase64(rawPayload));
        try {
            data = objectMapper().readValue(decodedJson, Map.class);
        } catch (Exception e) {
            LOG.warn("Failed to parse JSON session: " + decodedJson, e);
            return Collections.emptyMap();
        }

        try {
            if (!ObjectUtils.equals(data.get("algorithm"), "HMAC-SHA256")) {
                LOG.error("Unknown signature algorithm in signed_request: " + data.get("algorithm"));
                return Collections.emptyMap();
            }

            final Mac mac = Mac.getInstance("HMACSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(), mac.getAlgorithm()));
            final byte[] calcSig = mac.doFinal(asBytes(rawPayload));
            if (Arrays.equals(fromBase64(rawSignature), calcSig)) {
                return data;
            } else {

                LOG.warn("Signature does not match expected value: {} , Genned Sig: {}" , rawSignature, calcSig);

                return Collections.emptyMap();
            }

        } catch (Exception e) {
            LOG.warn("Failed to perform crypt operation: " + rawSignature, e);
            return Collections.emptyMap();
        }
    }

    private ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        return objectMapper;
    }

    private byte[] fromBase64(final String part) {
        return Base64.decodeBase64(filterUrlSafeBase64(part));
    }

    private byte[] filterUrlSafeBase64(final String base64Input) {
        final String filteredInput = base64Input.replace('_', '/').replace('-', '+');

        final byte[] source = asBytes(filteredInput);
        final int paddingOffset = source.length % 4;
        if (paddingOffset == 0) {
            return source;
        } else {
            final byte[] dest = new byte[source.length + paddingOffset];
            System.arraycopy(source, 0, dest, 0, source.length);
            for (int i = 1; i <= paddingOffset; ++i) {
                dest[dest.length - i] = '=';
            }
            return dest;
        }
    }

    private byte[] asBytes(final String source) {
        try {
            return source.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 isn't valid on this JVM. Chances of this happening? *shrugs*", e);
        }
    }

    private String asString(final byte[] source) {
        try {
            return new String(source, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 isn't valid on this JVM. Chances of this happening? *shrugs*", e);
        }
    }
}
