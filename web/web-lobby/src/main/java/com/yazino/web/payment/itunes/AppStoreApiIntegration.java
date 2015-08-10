package com.yazino.web.payment.itunes;

import com.yazino.web.util.JsonHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Integrates with Apple to determine if a purchase has legitimately been made.
 */
public class AppStoreApiIntegration {
    private static final Logger LOG = LoggerFactory.getLogger(AppStoreApiIntegration.class);

    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";

    private static final String STATUS = "status";
    private static final String RECEIPT = "receipt";
    private static final String RECEIPT_DATA = "receipt-data";

    private static final String RECEIPT_PRODUCT_ID = "product_id";
    private static final String RECEIPT_TRANSACTION_ID = "transaction_id";

    private static final long STATUS_CODE_SANDBOX_RECEIPT_INVALID_IN_PRODUCTION = 21007;

    private URI appStoreURI = URI.create("https://buy.itunes.apple.com/verifyReceipt");
    private URI sandboxAppStoreURI = URI.create("https://sandbox.itunes.apple.com/verifyReceipt");

    private JsonHelper jsonHelper = new JsonHelper();

    /**
     * Retrieves the order associated with the specified receipt.
     *
     * @param receipt not null
     * @return an {@link AppStoreOrder} object which will always have a StatusCode.
     * @throws java.io.IOException should the retrieval fail
     */
    public AppStoreOrder retrieveOrder(final String receipt) throws IOException {
        return retrieveOrder(receipt, HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec("ignoreCookies").build())
                .build());
    }

    final AppStoreOrder retrieveOrder(final String transactionReceipt,
                                      final CloseableHttpClient client) throws IOException {

        try {
            AppStoreOrder order = this.retrieveOrder(transactionReceipt, client, appStoreURI);
            if (order.isValid() || order.getStatusCode() != STATUS_CODE_SANDBOX_RECEIPT_INVALID_IN_PRODUCTION) {
                LOG.debug("Returning order from Production App Store");
                return order;
            }
            LOG.debug("Validating order against Sandbox App Store");
            return this.retrieveOrder(transactionReceipt, client, sandboxAppStoreURI);
        } finally {
            client.close();
        }
    }

    private AppStoreOrder retrieveOrder(final String transactionReceipt,
                                        final HttpClient client,
                                        final URI storeURI) throws IOException {
        final Map<String, Object> params = toRequestMap(transactionReceipt);

        final HttpPost httpPost = new HttpPost(storeURI);
        final String requestJson = jsonHelper.serialize(params);
        httpPost.setEntity(new StringEntity(requestJson));

        final StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        try {
            final HttpResponse response = client.execute(httpPost);

            if (response == null) {
                return new AppStoreOrder(-1, storeURI.equals(sandboxAppStoreURI));
            }

            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } finally {
            closeQuietly(reader);
        }

        final String responseJson = builder.toString();

        LOG.debug("Response for receipt {} from apple {} was {}", transactionReceipt, storeURI.toString(), responseJson);

        final Map result = jsonHelper.deserialize(Map.class, responseJson);

        final int status;
        if (result.containsKey(STATUS)) {
            status = (Integer) result.get(STATUS);
        } else {
            status = -1;
        }
        final Map receipt;
        if (result.containsKey(RECEIPT)) {
            receipt = (Map) result.get(RECEIPT);
        } else {
            receipt = Collections.emptyMap();
        }

        final AppStoreOrder order = new AppStoreOrder(status, storeURI.equals(sandboxAppStoreURI));
        order.setMessage(toMessage(params, result));

        if (order.isValid()) {
            order.setProductId((String) receipt.get(RECEIPT_PRODUCT_ID));
            order.setOrderId((String) receipt.get(RECEIPT_TRANSACTION_ID));
        }

        return order;
    }

    private String toMessage(final Map request, final Map response) {
        final Map<String, Object> map = new HashMap<>();
        map.put(REQUEST, request);
        map.put(RESPONSE, response);
        return jsonHelper.serialize(map);
    }

    private static void closeQuietly(final Closeable closeable) {
        try {
            closeable.close();
        } catch (Throwable e) {
            LOG.trace("failed to close {}, {} was thrown", closeable, e.getMessage());
        }
    }

    public static Map<String, Object> toRequestMap(final String receiptData) {
        final Map<String, Object> request = new HashMap<>();
        request.put(RECEIPT_DATA, receiptData);
        return request;
    }

}
