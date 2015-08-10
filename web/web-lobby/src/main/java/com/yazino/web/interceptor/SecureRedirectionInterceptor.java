package com.yazino.web.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

public class SecureRedirectionInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(SecureRedirectionInterceptor.class);

    private static final int HTTP_LENGTH = 4;

    private final boolean redirectToHttps;
    private final Collection<String> allowedPaths;

    @Autowired
    public SecureRedirectionInterceptor(@Value("${strata.web.protocol}") final String webProtocol,
                                        final Collection<String> allowedPaths) {
        this.allowedPaths = allowedPaths;
        this.redirectToHttps = webProtocol != null && "https".equals(webProtocol);
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler)
            throws Exception {
        if (redirectToHttps
                && !request.isSecure()
                && "GET".equals(request.getMethod())
                && "http".equals(request.getScheme())
                && !isAllowedWithoutRedirect(request)) {
            final StringBuffer requestURL = secureUrlOf(request);
            response.sendRedirect(requestURL.toString());
            LOG.debug("Requesting secure redirect for path {}", requestURL);
            return false;
        }

        return true;
    }

    private boolean isAllowedWithoutRedirect(final HttpServletRequest request) {
        final String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            for (String allowedPath : allowedPaths) {
                if (pathInfo.startsWith(allowedPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private StringBuffer secureUrlOf(final HttpServletRequest request) {
        final StringBuffer requestURL = request.getRequestURL().insert(HTTP_LENGTH, 's');
        if (request.getQueryString() != null) {
            requestURL.append("?")
                    .append(request.getQueryString());
        }
        return requestURL;
    }

}
