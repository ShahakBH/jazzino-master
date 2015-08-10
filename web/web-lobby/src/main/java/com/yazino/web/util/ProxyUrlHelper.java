package com.yazino.web.util;

import javax.servlet.http.HttpServletRequest;

public class ProxyUrlHelper {

    private String hostName;

    public ProxyUrlHelper(final String targetHostName) {
        this.hostName = targetHostName;
    }

    public boolean isProxiedRequest(final HttpServletRequest request) {
        return !hostName.equals(request.getHeader("host"));
    }

    public String createNonProxiedUrlFor(final HttpServletRequest request) {
        String protocol;
        if (request.isSecure()) {
            protocol = "https";
        } else {
            protocol = "http";
        }
        return protocol + "://" + hostName + toLocalRelativeUri(request.getRequestURI());
    }

    private String toLocalRelativeUri(final String uriPath) {
        final String[] uriParts = uriPath.split("/");
        String redirectUriPath = uriPath;
        final String proxyContextPath = uriParts[1];
        if (uriParts.length > 1 && hostName.startsWith(proxyContextPath)) {
            final int startOfRemainder = ("/" + proxyContextPath).length();
            redirectUriPath = uriPath.substring(startOfRemainder);
        }
        return redirectUriPath;
    }

}
