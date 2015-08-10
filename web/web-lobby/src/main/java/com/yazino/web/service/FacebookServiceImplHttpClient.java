package com.yazino.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * @see "https://developers.facebook.com/docs/authentication/server-side/"
 */
@Service("facebookService")
public class FacebookServiceImplHttpClient implements FacebookService {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookServiceImplHttpClient.class);

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("access_token=([\\w\\d]+)");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    static {
        JSON_MAPPER.registerModule(new JodaModule());
    }

    @Autowired
    public FacebookServiceImplHttpClient(@Qualifier("facebookHttpClient") final HttpClient httpClient) {
        notNull(httpClient, "httpClient can't be null");

        this.httpClient = httpClient;
    }

    @Override
    public String getAccessTokenForGivenCode(final String code,
                                             final String appId,
                                             final String appSecret,
                                             final String redirectUri) {
        notNull(code, "code may not be null");
        notNull(appId, "appId may not be null");
        notNull(appSecret, "appSecret may not be null");
        notNull(redirectUri, "redirectUri may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting access token for code={}; appId={}, appSecret={}, redirectUri={}",
                    code, appId, appSecret, redirectUri);
        }

        try {
            return getAccessTokenForHttpGet(new HttpGet(uriFor(code, appId, appSecret, redirectUri)));

        } catch (FacebookOAuthException e) {
            throw e;

        } catch (Exception ex) {
            throw new RuntimeException(String.format("Could not get access token: code=%s; appId=%s; appSecret=%s; redirectUri=%s",
                    code, appId, appSecret, redirectUri), ex);
        }
    }

    String getAccessTokenForHttpGet(final HttpGet httpGet) {
        return makeHttpCall(httpGet, new FBCallSuccessHandler() {
            @Override
            public String parseSuccessResponse(final HttpResponse httpResponse) throws IOException {
                return parseAccessTokenSuccessResponse(httpResponse);
            }
        });
    }

    private String makeHttpCall(final HttpGet httpGet, final FBCallSuccessHandler handler) {
        try {
            final HttpResponse httpResponse = httpClient.execute(httpGet);
            final int statusCode = httpResponse.getStatusLine().getStatusCode();
            switch (statusCode) {
                case HttpStatus.SC_OK:
                    return handler.parseSuccessResponse(httpResponse);

                case HttpStatus.SC_BAD_REQUEST:
                    return parseFailureResponse(httpResponse);

                default:
                    throw new RuntimeException(String.format("HTTP Status was: %s; body=%s",
                            statusCode, getResponseBody(httpResponse)));
            }
        } catch (RuntimeException ex) {
            httpGet.abort();
            throw ex;

        } catch (IOException ex) {
            httpGet.abort();
            throw new RuntimeException("IOException during Facebook access token query", ex);
        }
    }

    private String parseFailureResponse(final HttpResponse httpResponse) throws IOException {
        final String responseBody = getResponseBody(httpResponse);
        final JsonNode errorDoc = JSON_MAPPER.readTree(responseBody);
        if (errorDoc.has("error")) {
            final JsonNode errorChild = errorDoc.get("error");
            throw new FacebookOAuthException(
                    getNodeValue(errorChild, "type"), getNodeValue(errorChild, "message"));
        }
        throw new RuntimeException(String.format("Bad request received with malformed body; body=%s",
                responseBody));
    }

    private interface FBCallSuccessHandler {
        String parseSuccessResponse(final HttpResponse httpResponse) throws IOException;
    }

    private String parseAccessTokenSuccessResponse(final HttpResponse httpResponse) throws IOException {
        final String result = getResponseBody(httpResponse);
        final Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(result);
        if (matcher.find()) {
            return matcher.toMatchResult().group(1);
        }
        throw new RuntimeException(String.format("Error parsing AccessTokenResponse:'%s'", result));
    }

    private String getNodeValue(final JsonNode node,
                                final String field) {
        if (node.has(field)) {
            return node.get(field).textValue();
        }
        return null;
    }

    private String getResponseBody(final HttpResponse httpResponse) throws IOException {
        final HttpEntity entity = httpResponse.getEntity();
        final String responseBody = EntityUtils.toString(entity, "UTF-8");

        LOG.debug("Received body: {}", responseBody);

        if (responseBody == null) {
            return "";
        }
        return responseBody;
    }

    private URI uriFor(final String code,
                       final String appId,
                       final String appSecret,
                       final String redirectUri) throws URISyntaxException {
        final List<NameValuePair> getParams = new ArrayList<>();
        getParams.add(new BasicNameValuePair("client_id", appId));
        getParams.add(new BasicNameValuePair("client_secret", appSecret));
        getParams.add(new BasicNameValuePair("redirect_uri", redirectUri));
        getParams.add(new BasicNameValuePair("code", code));
        return URIUtils.createURI("https", "graph.facebook.com", -1, "/oauth/access_token",
                URLEncodedUtils.format(getParams, "UTF-8"), null);
    }
}
