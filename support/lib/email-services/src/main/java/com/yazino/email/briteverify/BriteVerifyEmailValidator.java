package com.yazino.email.briteverify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailValidator;
import com.yazino.email.EmailVerificationResult;
import com.yazino.email.EmailVerificationStatus;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;

import static com.yazino.email.EmailVerificationStatus.MALFORMED;
import static com.yazino.email.EmailVerificationStatus.UNKNOWN_TEMPORARY;
import static org.apache.commons.lang3.Validate.notNull;

@Service("briteVerifyEmailValidator")
public class BriteVerifyEmailValidator implements EmailValidator {
    private static final Logger LOG = LoggerFactory.getLogger(BriteVerifyEmailValidator.class);

    private static final String PROPERTY_API_KEY = "email.briteverify.api-key";
    private static final String PROPERTY_URL = "email.briteverify.url";
    private static final int HTTP_STATUS_OKAY = 200;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final YazinoConfiguration yazinoConfiguration;
    private final HttpClient httpClient;

    @Autowired
    public BriteVerifyEmailValidator(@Qualifier("briteVerifyHttpClient") final HttpClient httpClient,
                                     final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(httpClient, "httpClient may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.httpClient = httpClient;
    }

    @Override
    public EmailVerificationResult validate(final String address) {
        notNull(address, "address may not be null");

        final int hostIndex = address.indexOf("@");
        if (hostIndex == -1) {
            LOG.debug("Email address is malformed: {}", address);
            return new EmailVerificationResult(address, MALFORMED);
        }

        final String username = address.substring(0, hostIndex);
        if (StringUtils.containsWhitespace(username)) {
            LOG.debug("Email address is malformed: {}", address);
            return new EmailVerificationResult(address, MALFORMED);
        }

        final String hostname = address.substring(hostIndex + 1);
        if (StringUtils.isBlank(hostname)) {
            LOG.debug("Email address is malformed: {}", address);
            return new EmailVerificationResult(address, MALFORMED);
        }

        return validateWithBriteVerify(address);
    }

    private EmailVerificationResult validateWithBriteVerify(final String address) {
        try {
            final HttpResponse response = httpClient.execute(requestFor(address));
            if (response.getStatusLine().getStatusCode() != HTTP_STATUS_OKAY) {
                LOG.error("Verification request for {} failed with status code {} and response {}",
                        address, response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity()));
                return new EmailVerificationResult(address, UNKNOWN_TEMPORARY);
            }

            return handleResponse(address, objectMapper.readTree(response.getEntity().getContent()));

        } catch (URISyntaxException e) {
            LOG.error("BriteVerify URL is invalid", e);
            return new EmailVerificationResult(address, UNKNOWN_TEMPORARY);

        } catch (Exception e) {
            LOG.error("BriteVerify verification failed for address {}", address, e);
            return new EmailVerificationResult(address, UNKNOWN_TEMPORARY);
        }
    }

    private EmailVerificationResult handleResponse(final String address, final JsonNode responseTree) {
        LOG.debug("Response is {}", responseTree);

        final EmailVerificationStatus result;
        final String status = stringValueOf(responseTree, "status");
        if (ObjectUtils.equals(status, "valid")) {
            LOG.debug("Address is valid: {}", address);
            result = EmailVerificationStatus.VALID;

        } else if (ObjectUtils.equals(status, "unknown")) {
            LOG.debug("Address is unknown: {}", address);
            result = EmailVerificationStatus.UNKNOWN;

        } else if (ObjectUtils.equals(status, "accept_all")) {
            LOG.debug("Address domain accepts all: {}", address);
            result = EmailVerificationStatus.ACCEPT_ALL;

        } else {
            LOG.debug("Address is invalid: {}; error: {}", address, stringValueOf(responseTree, "error_code"));
            result = EmailVerificationStatus.INVALID;
        }

        return new EmailVerificationResult(address, result,
                ObjectUtils.equals(booleanValueOf(responseTree, "disposable"), true),
                ObjectUtils.equals(booleanValueOf(responseTree, "role_address"), true));
    }

    private String stringValueOf(final JsonNode responseTree, final String fieldName) {
        final JsonNode field = responseTree.get(fieldName);
        if (field != null) {
            return field.textValue();
        }
        return "";
    }

    private Boolean booleanValueOf(final JsonNode responseTree, final String fieldName) {
        final JsonNode field = responseTree.get(fieldName);
        if (field != null && field.isBoolean()) {
            return field.booleanValue();
        }
        return null;
    }

    private HttpGet requestFor(final String address) throws URISyntaxException {
        final String url = yazinoConfiguration.getString(PROPERTY_URL);
        if (url == null) {
            throw new IllegalStateException("No URL is set for BriteVerify: " + PROPERTY_URL);
        }

        final String apiKey = yazinoConfiguration.getString(PROPERTY_API_KEY);
        if (apiKey == null) {
            throw new IllegalStateException("No API Key is set for BriteVerify: " + PROPERTY_API_KEY);
        }

        final URIBuilder builder = new URIBuilder(url)
                .setParameter("address", address)
                .setParameter("apikey", apiKey);

        return new HttpGet(builder.build());
    }
}
