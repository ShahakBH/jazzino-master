package com.yazino.bi.opengraph;

import com.google.common.base.Charsets;
import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.opengraph.OpenGraphAction;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenGraphHttpInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(OpenGraphHttpInvoker.class);

    private static final String FB_REF_PREFIX = "fb_og_";
    static final String OBJECT_URL_FORMAT = "%s/fbog/%s/%s";

    private HttpClient httpClient;
    private String openGraphObjectsHost;

    public OpenGraphHttpInvoker() {
    }

    @Autowired
    public OpenGraphHttpInvoker(@Qualifier("openGraphHttpClient") final HttpClient httpClient,
                                @Value("${facebook.openGraphObjectsHost}") final String openGraphObjectsHost) {
        this.httpClient = httpClient;
        this.openGraphObjectsHost = openGraphObjectsHost;
    }

    // TODO test response parsing, etc.
    public void publishAction(final String accessToken,
                              final OpenGraphAction action,
                              final String appNamespace) throws IOException {
        final HttpPost post = new HttpPost("https://graph.facebook.com/me/" + appNamespace + ":" + action.getName());

        final List<NameValuePair> formData = new ArrayList<>();
        formData.add(new BasicNameValuePair("access_token", accessToken));
        formData.add(new BasicNameValuePair(action.getObject().getType(), buildUrl(action)));
        formData.add(new BasicNameValuePair("ref", FB_REF_PREFIX + action.getObject().getId()));

        post.setEntity(new UrlEncodedFormEntity(formData, Charsets.UTF_8));

        final HttpResponse response = httpClient.execute(post);
        LOG.debug("Publishing to OpenGraph(action={},appNamespace={})", action, appNamespace);

        final String json = readStream(response);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            LOG.debug("Published successfully.  Response is: {}", json);
        } else {
            LOG.warn("Unable to publish action: {}", json);
            throw new InvalidAccessTokenException(json);
        }

    }

    private String buildUrl(final OpenGraphAction action) {
        return String.format(OBJECT_URL_FORMAT,
                openGraphObjectsHost,
                action.getObject().getType(),
                action.getObject().getId());
    }

    @Cacheable(cacheName = "openGraphPermissionCache")
    public boolean hasPermission(final String accessToken) {
        try {
            final HttpGet request = new HttpGet("https://graph.facebook.com/me/permissions?access_token="
                    + accessToken);
            return readStream(httpClient.execute(request)).contains("\"publish_actions\":1");
        } catch (Exception e) {
            LOG.warn("Unable to check permission.", e);
            return false;
        }
    }

    private static String readStream(final HttpResponse response) throws IOException {
        BufferedReader reader = null;
        try {
            final StringBuilder builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                LOG.warn("Unable to close stream", e);
            }
        }
    }
}
