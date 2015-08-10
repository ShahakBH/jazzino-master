package com.yazino.web.payment.amazon;

import com.yazino.configuration.YazinoConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.yazino.web.payment.amazon.VerificationResult.VALID;

@Component
public class AmazonReceiptVerificationService implements ReceiptVerification {
    private static final String AMAZON_RVS_ENABLED_PROPERTY = "amazon.rvs.enabled";
    private static final String AMAZON_RVS_HOST_PROPERTY = "amazon.rvs.host";
    private static final String AMAZON_RVS_DEVELOPER_SECRET_PROPERTY = "amazon.rvs.developerSecret";
    private static final String URL_TEMPLATE = "%s/version/2.0/verify/developer/%s/user/%s/purchaseToken/%s";
    private final YazinoConfiguration yazinoConfiguration;
    private final HttpClient client;

    @Autowired
    public AmazonReceiptVerificationService(YazinoConfiguration yazinoConfiguration, @Qualifier("amazonHttpClient") HttpClient client) {
        this.yazinoConfiguration = yazinoConfiguration;
        this.client = client;
    }

    @Override
    public VerificationResult verify(String userId, String purchaseToken) throws IOException {
        if (yazinoConfiguration.getBoolean(AMAZON_RVS_ENABLED_PROPERTY)) {
            final HttpGet request = new HttpGet(buildUrl(userId, purchaseToken));
            final HttpResponse response = client.execute(request);
            final int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consume(response.getEntity());
            return VerificationResult.fromStatusCode(statusCode);
        }
        return VALID;
    }

    private String buildUrl(String userId, String purchaseToken) {
        return String.format(URL_TEMPLATE,
                yazinoConfiguration.getString(AMAZON_RVS_HOST_PROPERTY),
                yazinoConfiguration.getString(AMAZON_RVS_DEVELOPER_SECRET_PROPERTY),
                userId,
                purchaseToken);
    }
}
