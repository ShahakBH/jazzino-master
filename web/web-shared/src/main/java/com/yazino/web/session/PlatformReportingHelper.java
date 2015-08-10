package com.yazino.web.session;

import javax.servlet.http.HttpServletRequest;

public final class PlatformReportingHelper {

    public static final String REQUEST_URL = "YazinoRequestStartPage";

    private PlatformReportingHelper() {
    }

    public static String getRequestUrl(HttpServletRequest request) {
        final StringBuffer requestURL = request.getRequestURL();
        if (requestURL == null) {
            return null;
        }
        String url = requestURL.toString();
        if (request.getAttribute(REQUEST_URL) != null) {
            url = (String) request.getAttribute(REQUEST_URL);
        }
        return url;
    }
}
