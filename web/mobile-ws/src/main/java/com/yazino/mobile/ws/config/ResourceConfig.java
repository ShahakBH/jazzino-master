package com.yazino.mobile.ws.config;

import org.apache.commons.lang3.Validate;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ResourceConfig {

    static final String STATIC = "/static";

    private String mBaseUrl = defaultUrl();
    private String mContentUrl = mBaseUrl + STATIC;

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public void setBaseUrl(final String baseUrl) {
        Validate.notNull(baseUrl);
        mBaseUrl = baseUrl;
    }

    public String getContentUrl() {
        return mContentUrl;
    }

    public void setContentUrl(final String contentUrl) {
        Validate.notNull(contentUrl);
        mContentUrl = contentUrl;
    }

    private static String defaultUrl() {
        try {
            return String.format("http://%s", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Failed to configure default resource base url", e);
        }
    }
}
