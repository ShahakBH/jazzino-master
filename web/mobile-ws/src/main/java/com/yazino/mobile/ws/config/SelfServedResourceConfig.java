package com.yazino.mobile.ws.config;

import org.apache.commons.lang3.Validate;

import javax.servlet.http.HttpServletRequest;

/**
 * A subclass of {@link ResourceConfig} that populates itself using a {@link javax.servlet.http.HttpServletRequest}.
 */
public class SelfServedResourceConfig extends ResourceConfig {

    public SelfServedResourceConfig(final HttpServletRequest request, final String suffix) {
        Validate.notNull(request);
        Validate.notNull(suffix);
        String baseUrl = selfServedContentUrl(request);
        setBaseUrl(baseUrl);
        setContentUrl(baseUrl + STATIC + suffix);
    }

    private static String selfServedContentUrl(final HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String contextPath = request.getContextPath();
        return requestURL.substring(0, requestURL.indexOf(contextPath) + contextPath.length());
    }


}
