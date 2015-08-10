package com.yazino.mobile.ws.config;

import org.apache.commons.lang3.Validate;

import javax.servlet.http.HttpServletRequest;

/**
 * Respresents {@link ResourceConfig} that resides on the same host but at a different context.
 */
public class SameHostDifferentContextResourceConfig extends ResourceConfig {

    public SameHostDifferentContextResourceConfig(final HttpServletRequest request, final String suffix) {
        Validate.notNull(request);
        Validate.notNull(suffix);
        String baseUrl = everythingExcludingContext(request);
        setBaseUrl(baseUrl);
        setContentUrl(baseUrl + suffix);
    }

    private static String everythingExcludingContext(final HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String contextPath = request.getContextPath();
        return requestURL.substring(0, requestURL.indexOf(contextPath));
    }

}
