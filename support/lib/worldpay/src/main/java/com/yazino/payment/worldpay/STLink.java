package com.yazino.payment.worldpay;

import com.google.common.base.Charsets;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.payment.worldpay.NVPXmlResponse.isXmlResponse;
import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

@Service("stLink")
public class STLink implements ConfigurationListener {
    private static final Logger LOG = LoggerFactory.getLogger(STLink.class);

    private static final String PROPERTY_3D_SECURE_ENABLED = "payment.worldpay.stlink.3dsecure.enabled";
    private static final String PROPERTY_GATEWAY = "payment.worldpay.stlink.gateway";
    private static final String PROPERTY_MERCHANT_DEFAULT_ID = "payment.worldpay.stlink.merchant.default";
    private static final String PROPERTY_MERCHANT_LIST_ID = "payment.worldpay.stlink.merchant";
    private static final String PROPERTY_MERCHANT_STORE_ID = "payment.worldpay.stlink.merchant.%s.%s.storeid";
    private static final String PROPERTY_MERCHANT_3DSECURE_STORE_ID = "payment.worldpay.stlink.merchant.%s.%s.storeid.3dsecure";
    private static final String PROPERTY_MERCHANT_CURRENCY_ID = "payment.worldpay.stlink.merchant.%s.currencies";
    private static final String PROPERTY_PT_USERNAME = "payment.worldpay.stlink.pt.username";
    private static final String PROPERTY_PT_PASSWORD = "payment.worldpay.stlink.pt.password";
    private static final String PROPERTY_3D_USERNAME = "payment.worldpay.stlink.3d.username";
    private static final String PROPERTY_3D_PASSWORD = "payment.worldpay.stlink.3d.password";
    private static final String PROPERTY_RG_USERNAME = "payment.worldpay.stlink.rg.username";
    private static final String PROPERTY_RG_PASSWORD = "payment.worldpay.stlink.rg.password";
    private static final String PROPERTY_RD_USERNAME = "payment.worldpay.stlink.rd.username";
    private static final String PROPERTY_RD_PASSWORD = "payment.worldpay.stlink.rd.password";

    private final Map<Integer, Merchant> merchantsByCurrency = new HashMap<>();

    private final YazinoConfiguration config;
    private final HttpClient httpClient;

    @Autowired
    public STLink(@Qualifier("stLinkHttpClient") final HttpClient httpClient,
                  final YazinoConfiguration yazinoConfiguration) {
        notNull(httpClient, "httpClient may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.httpClient = httpClient;
        this.config = yazinoConfiguration;

        merchantsByCurrency.putAll(loadMerchantsFromConfiguration());
        config.addConfigurationListener(this);
    }

    private NVPMessage addMerchantInfoTo(final NVPMessage message) {
        notNull(message, "nvpMessage may not be null");

        String usernameProperty;
        String passwordProperty;
        switch (message.getTransactionType()) {
            case "PT":
                usernameProperty = PROPERTY_PT_USERNAME;
                passwordProperty = PROPERTY_PT_PASSWORD;
                break;
            case "3D":
                usernameProperty = PROPERTY_3D_USERNAME;
                passwordProperty = PROPERTY_3D_PASSWORD;
                break;
            case "RG":
                usernameProperty = PROPERTY_RG_USERNAME;
                passwordProperty = PROPERTY_RG_PASSWORD;
                break;
            case "RD":
                usernameProperty = PROPERTY_RD_USERNAME;
                passwordProperty = PROPERTY_RD_PASSWORD;
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction type: " + message.getTransactionType());
        }

        final Merchant merchant = merchantFor(message);
        final NVPMessage populatedMessage = message.withValue("MerchantId", merchant.getMerchantId())
                .withValue("UserName", config.getString(usernameProperty))
                .withValue("UserPassword", config.getString(passwordProperty));

        if (config.getBoolean(PROPERTY_3D_SECURE_ENABLED, false)) {
            return populatedMessage.withValueIfNotAlreadySet("StoreID", merchant.getStoreId3dSecure());
        }
        return populatedMessage.withValueIfNotAlreadySet("StoreID", merchant.getStoreId());
    }

    public NVPResponse send(final NVPMessage nvpMessage) {
        notNull(nvpMessage, "nvpMessage may not be null");
        final NVPMessage messageWithAuth = addMerchantInfoTo(nvpMessage);
        return new NVPResponse(messageWithAuth.toObscuredMessage(), doSend(messageWithAuth));
    }

    public String sendWithoutParsing(final NVPMessage nvpMessage) {
        notNull(nvpMessage, "nvpMessage may not be null");
        return doSend(addMerchantInfoTo(nvpMessage));
    }

    public NVPXmlResponse expectXMLSend(final NVPMessage nvpMessage) {
        notNull(nvpMessage, "nvpMessage may not be null");
        final NVPMessage messageWithAuth = addMerchantInfoTo(nvpMessage);
        final String response = doSend(messageWithAuth);
        if (isXmlResponse(response)) {
            return new NVPXmlResponse(response);
        } else {
            throw new IllegalStateException(format("Non XML response: %s", response));
        }
    }

    private String doSend(final NVPMessage messageWithAuth) {
        if (!messageWithAuth.isValid()) {
            LOG.debug("Called attempted to send invalid nvpMessage: {}", messageWithAuth);
            throw new IllegalStateException("Message is invalid: " + messageWithAuth);
        }

        HttpResponse response = null;
        final String responseBody;
        try {
            response = httpClient.execute(postWithMessage(messageWithAuth));
            if (response == null) {
                throw new IllegalStateException("HttpClient returned a null response for " + messageWithAuth);
            }

            responseBody = EntityUtils.toString(response.getEntity(),
                    ContentType.getOrDefault(response.getEntity()).getCharset());

        } catch (Exception e) {
            LOG.error("Remote request failed for message {} with status {}", messageWithAuth, statusCodeFrom(response), e);
            throw new RuntimeException("Remote request failed for message " + messageWithAuth, e);
        }
        return responseBody;
    }

    private int statusCodeFrom(final HttpResponse response) {
        if (response == null || response.getStatusLine() == null) {
            return -1;
        }
        return response.getStatusLine().getStatusCode();
    }

    private HttpPost postWithMessage(final NVPMessage nvpMessage) throws IOException {
        final HttpPost httpPost = new HttpPost(config.getString(PROPERTY_GATEWAY));
        httpPost.setEntity(new StringEntity(nvpMessage.toMessage(), Charsets.UTF_8));
        return httpPost;
    }

    private Map<Integer, Merchant> loadMerchantsFromConfiguration() {
        final Map<Integer, Merchant> merchants = new HashMap<>();
        for (String merchantId : config.getStringArray(PROPERTY_MERCHANT_LIST_ID)) {
            for (String currencyCode : config.getStringArray(format(PROPERTY_MERCHANT_CURRENCY_ID, merchantId))) {
                final int storeId = config.getInt(format(PROPERTY_MERCHANT_STORE_ID, merchantId, currencyCode));
                final int storeId3dSecure = config.getInt(format(PROPERTY_MERCHANT_3DSECURE_STORE_ID, merchantId, currencyCode), storeId);
                merchants.put(Currency.getInstance(currencyCode).getNumericCode(),
                        new Merchant(Integer.parseInt(merchantId), storeId, storeId3dSecure));
            }
        }
        return merchants;
    }

    private Merchant merchantFor(final NVPMessage message) {
        final Integer messageCurrencyCode = message.getCurrencyId();

        if (messageCurrencyCode == null) {
            for (Merchant merchant : merchantsByCurrency.values()) {
                if (merchant.getMerchantId() == config.getInt(PROPERTY_MERCHANT_DEFAULT_ID)) {
                    return merchant;
                }
            }
            throw new IllegalStateException(format("Couldn't find default merchant %s for message without currency code: %s",
                    config.getInt(PROPERTY_MERCHANT_DEFAULT_ID), message.toObscuredMessage()));
        }

        final Merchant merchant = merchantsByCurrency.get(messageCurrencyCode);
        if (merchant != null) {
            return merchant;
        }

        throw new IllegalStateException("Couldn't find merchant for message: " + message.toObscuredMessage());
    }

    @Override
    public void configurationChanged(final ConfigurationEvent event) {
        synchronized (merchantsByCurrency) {
            merchantsByCurrency.clear();
            merchantsByCurrency.putAll(loadMerchantsFromConfiguration());
        }
    }

    private final class Merchant {
        private final int merchantId;
        private final int storeId;
        private final int storeId3dSecure;

        private Merchant(final int merchantId,
                         final int storeId,
                         final int storeId3dSecure) {
            this.merchantId = merchantId;
            this.storeId = storeId;
            this.storeId3dSecure = storeId3dSecure;
        }

        private int getMerchantId() {
            return merchantId;
        }

        private int getStoreId() {
            return storeId;
        }

        private int getStoreId3dSecure() {
            return storeId3dSecure;
        }
    }

}
