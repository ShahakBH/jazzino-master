package com.yazino.metrics.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.configuration.YazinoConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class MetricsClient {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsClient.class);

    private static final String PROPERTY_METRICS_SERVER = "metrics.aggregation.server";
    private static final String DEFAULT_METRICS_SERVER = "http://localhost:7900/metrics/metrics";
    private static final int HTTP_SUCCESSFUL = 200;
    private static final int HTTP_CLIENT_ERROR = 400;

    private final YazinoConfiguration config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public MetricsClient(@Qualifier("metricsHttpClient") final HttpClient httpClient,
                         final ObjectMapper objectMapper,
                         final YazinoConfiguration yazinoConfiguration) {
        notNull(httpClient, "httpClient may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(objectMapper, "objectMapper may not be null");

        this.httpClient = httpClient;
        this.config = yazinoConfiguration;
        this.objectMapper = objectMapper;
    }

    void send(final Map<String, Object> metricData) {
        if (metricData == null || metricData.isEmpty()) {
            return;
        }

        HttpResponse response = null;
        try {
            response = httpClient.execute(post(metricData));
            if (response == null) {
                throw new IllegalStateException("HttpClient returned a null response for " + metricData);
            }

            if (statusCodeFrom(response) < HTTP_SUCCESSFUL || statusCodeFrom(response) >= HTTP_CLIENT_ERROR) {
                LOG.warn("Unexpected status from metrics server: {}", statusCodeFrom(response));
            }

            EntityUtils.consume(response.getEntity());

        } catch (Exception e) {
            LOG.warn("Remote request failed for message {} with status {}", metricData, statusCodeFrom(response), e);
        }
    }

    private int statusCodeFrom(final HttpResponse response) {
        if (response == null || response.getStatusLine() == null) {
            return -1;
        }
        return response.getStatusLine().getStatusCode();
    }

    private HttpPost post(final Map<String, Object> json) throws IOException {
        final String metricsServer = config.getString(PROPERTY_METRICS_SERVER, DEFAULT_METRICS_SERVER);

        LOG.debug("Creating post to {} with client ID {}", metricsServer, json.get("clientId"));

        final HttpPost httpPost = new HttpPost(metricsServer);
        httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(json), ContentType.APPLICATION_JSON));
        httpPost.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        return httpPost;
    }
}
